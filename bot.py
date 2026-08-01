"""
ncbot — RemoteTerm integration script (async fire-and-forget).

RemoteTerm's bot system gives every bot() call a 10-second budget
(BOT_EXECUTION_TIMEOUT in app/fanout/bot_exec.py). That budget covers the
entire synchronous path, so a blocking call into ncbot can never survive a
slow AI response.

This script decouples AI latency from that timeout entirely:

  1. bot(**kwargs) returns None to RemoteTerm immediately (well under 10 s),
     so RemoteTerm's execution timeout never fires.
  2. The message is processed in a background daemon thread, which:
       a. POSTs the message to ncbot's /v1/chat endpoint with a long timeout
          (no RemoteTerm timeout applies to this thread), then
       b. POSTs each reply to RemoteTerm's *own* HTTP API
          (/api/messages/direct or /api/messages/channel) so it goes out on
          the mesh.

Deploy: paste this as the bot's `code` in the RemoteTerm bot config
(Frontend → Bot, or POST /api/fanout). The script runs inside the RemoteTerm
process, so it reads RemoteTerm's environment and can reach the API on
localhost. All knobs are env vars of the RemoteTerm process:

  NCBOT_URL                 ncbot /v1/chat endpoint
                            (default http://localhost:8080/v1/chat)
  NCBOT_TIMEOUT             seconds to wait for ncbot to answer
                            (default 1800 = 30 min)
  NCBOT_MAX_PENDING         max in-flight AI requests; beyond this, new
                            messages are dropped (default 50)
  RT_API_URL                RemoteTerm HTTP API base
                            (default http://localhost:8000/api)
  RT_API_TIMEOUT            seconds per RemoteTerm API call (default 30)
  NCBOT_MESSAGE_SPACING     min seconds between bot sends, so repeaters can
                            return to listening mode (default 2.0)

RemoteTerm's optional HTTP Basic auth (MESHCORE_BASIC_AUTH_USERNAME /
MESHCORE_BASIC_AUTH_PASSWORD) is picked up automatically when enabled.

Caveats: replies are best-effort — if RemoteTerm restarts while the AI call
is in flight, the reply is lost. There is no ordering guarantee relative to
other messages, and send spacing is enforced by this script rather than by
RemoteTerm.
"""

import base64
import json
import logging
import os
import threading
import time
import urllib.error
import urllib.request

logger = logging.getLogger("ncbot_async")

NCBOT_URL = os.environ.get("NCBOT_URL", "http://localhost:8080/v1/chat")
NCBOT_TIMEOUT = float(os.environ.get("NCBOT_TIMEOUT", "1800"))
NCBOT_MAX_PENDING = int(os.environ.get("NCBOT_MAX_PENDING", "50"))
RT_API_URL = os.environ.get("RT_API_URL", "http://localhost:8000/api").rstrip("/")
RT_API_TIMEOUT = float(os.environ.get("RT_API_TIMEOUT", "30"))
BOT_MESSAGE_SPACING = float(os.environ.get("NCBOT_MESSAGE_SPACING", "2.0"))


def _basic_auth_header() -> str | None:
    """Return an HTTP Basic auth header from RemoteTerm's env, if configured."""
    username = os.environ.get("MESHCORE_BASIC_AUTH_USERNAME", "")
    if not username:
        return None
    password = os.environ.get("MESHCORE_BASIC_AUTH_PASSWORD", "")
    token = base64.b64encode(f"{username}:{password}".encode("utf-8")).decode("ascii")
    return f"Basic {token}"


def _post(url: str, payload: dict, timeout: float) -> tuple[int, bytes]:
    """POST JSON; returns (status, body). Non-2xx responses are returned,
    not raised, so callers can inspect the status."""
    headers = {"Content-Type": "application/json"}
    auth = _basic_auth_header()
    if auth:
        headers["Authorization"] = auth
    request = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers=headers,
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as resp:
            return resp.status, resp.read()
    except urllib.error.HTTPError as exc:
        return exc.code, exc.read()


def _pending_adjust(shared: dict, delta: int) -> int:
    """Adjust the shared in-flight counter atomically; returns new value."""
    with shared.setdefault("ncbot_count_lock", threading.Lock()):
        count = shared.get("ncbot_pending", 0) + delta
        shared["ncbot_pending"] = count
        return count


def _send_reply_on_mesh(reply: str, kwargs: dict) -> None:
    """Send one reply through RemoteTerm's HTTP API, rate-limited.

    RemoteTerm's own bot send path applies 2 s spacing between sends
    (BOT_MESSAGE_SPACING); since we bypass it, we enforce the same spacing
    here using the shared _bot_globals state.
    """
    shared = _bot_globals
    lock = shared.setdefault("ncbot_send_lock", threading.Lock())

    is_dm = kwargs.get("is_dm", False)
    if is_dm:
        destination = kwargs.get("sender_key")
        if not destination:
            logger.warning("ncbot: DM reply dropped (no sender_key)")
            return
        url = f"{RT_API_URL}/messages/direct"
        payload = {"destination": destination, "text": reply}
    else:
        channel_key = kwargs.get("channel_key")
        if not channel_key:
            logger.warning("ncbot: channel reply dropped (no channel_key)")
            return
        url = f"{RT_API_URL}/messages/channel"
        payload = {"channel_key": channel_key, "text": reply}

    with lock:
        now = time.monotonic()
        last_send = shared.get("ncbot_last_send_time", 0.0)
        wait = BOT_MESSAGE_SPACING - (now - last_send)
        if last_send > 0 and wait > 0:
            time.sleep(wait)
        try:
            status, _ = _post(url, payload, RT_API_TIMEOUT)
            if not 200 <= status < 300:
                logger.warning("ncbot: RemoteTerm API returned HTTP %d for reply", status)
            else:
                shared["ncbot_last_send_time"] = time.monotonic()
        except Exception:
            logger.exception("ncbot: failed to send reply via RemoteTerm API")


def bot(**kwargs) -> None:
    """
    Process messages without blocking RemoteTerm's bot timeout.

    Returns None immediately and does the real work in a background daemon
    thread: ncbot /v1/chat, then RemoteTerm's send API for each reply.

    Args (from RemoteTerm):
        sender_name: Display name of sender (nullable)
        sender_key: Hex public key (nullable for channels)
        message_text: The message content
        is_dm: True for DMs, false for channels
        channel_key: Hex channel key (nullable for DMs)
        channel_name: Channel name with hash (nullable for DMs)
        sender_timestamp: Unix seconds (nullable)
        path: Hex-encoded routing path (nullable)
        is_outgoing: Whether this is our own outgoing message
        path_bytes_per_hop: 1, 2, or 3 (nullable)
        packet_hash: MeshCore packet hash, first 16 hex chars of SHA256 (nullable)
        region: Decoded region name; only meaningful when scoped is True (nullable)
        scoped: True if message carried a regional flood scope (bool)

    Returns:
        Always None. Replies are delivered asynchronously on the mesh.
    """
    # Don't reply to our own outgoing messages
    if kwargs.get("is_outgoing", False):
        return None

    message_text = kwargs.get("message_text", "")
    if not message_text:
        return None

    payload = {
        "senderName": kwargs.get("sender_name"),
        "senderKey": kwargs.get("sender_key"),
        "messageText": message_text,
        "isDm": kwargs.get("is_dm"),
        "channelKey": kwargs.get("channel_key"),
        "channelName": kwargs.get("channel_name"),
        "senderTimestamp": kwargs.get("sender_timestamp"),
        "path": kwargs.get("path"),
        "isOutgoing": kwargs.get("is_outgoing", False),
        "pathBytesPerHop": kwargs.get("path_bytes_per_hop"),
        "packetHash": kwargs.get("packet_hash"),
        "region": kwargs.get("region"),
        "scoped": kwargs.get("scoped", False),
    }

    shared = _bot_globals

    # Guard against unbounded thread growth on busy channels: each pending
    # request pins a thread for up to NCBOT_TIMEOUT seconds.
    if _pending_adjust(shared, 1) > NCBOT_MAX_PENDING:
        _pending_adjust(shared, -1)
        logger.warning("ncbot: dropping message (%d requests already pending)", NCBOT_MAX_PENDING)
        return None

    def _process() -> None:
        try:
            try:
                status, body = _post(NCBOT_URL, payload, NCBOT_TIMEOUT)
                if not 200 <= status < 300:
                    logger.warning("ncbot: /v1/chat returned HTTP %d", status)
                    return
                result = json.loads(body)
            except Exception:
                logger.exception("ncbot: /v1/chat request failed")
                return

            replies = result.get("replies", [])
            if isinstance(replies, str):
                replies = [replies]
            for reply in replies:
                if isinstance(reply, str) and reply.strip():
                    _send_reply_on_mesh(reply, kwargs)
        finally:
            _pending_adjust(shared, -1)

    thread = threading.Thread(target=_process, name="ncbot-async", daemon=True)
    thread.start()
    return None
