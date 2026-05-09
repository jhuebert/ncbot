"""
ncbot — RemoteTerm integration script.

Receives kwargs from RemoteTerm's bot system, POSTs to ncbot's /v1/chat endpoint,
and returns the AI-generated response(s).

Usage:
    Place this file in your RemoteTerm bot directory.
    Set NCBOT_URL environment variable to point at your ncbot instance.
    Default: http://localhost:8080/v1/chat
"""

import json
import os
import urllib.request


NCBOT_URL = os.environ.get("NCBOT_URL", "http://localhost:8080/v1/chat")


def bot(**kwargs) -> str | list[str] | None:
    """
    Process messages and optionally return a reply.

    Forwards the message to the ncbot API for AI processing.

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

    Returns:
        None — no reply
        str — single reply
        list[str] — multiple replies
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
    }

    try:
        req = urllib.request.Request(
            NCBOT_URL,
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=9) as resp:
            result = json.loads(resp.read())
            replies = result.get("replies", [])
            return replies if replies else None
    except Exception:
        return None
