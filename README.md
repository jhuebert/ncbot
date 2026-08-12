# ncbot

AI chat provider for [Meshcore](https://meshcore.io/), invoked by the RemoteTerm application for every message sent to a chat channel or DM thread. It persists messages to SQLite, runs them through an OpenAI-compatible AI model, and returns short responses (≤ 128 UTF-8 bytes) that RemoteTerm delivers back to the mesh network.

## Architecture

```mermaid
flowchart TD
    RT["RemoteTerm (Python bot)"] -->|POST /v1/chat| CC["ChatController"]

    subgraph subbot ["ncbot (Spring Boot)"]
        CC --> CS["ChatService"]
        CS --> HC["ChatHandler Chain (ordered)"]
        HC --> BH["BlockingChatHandler"]
        BH -->|blocked| SKIP["Short-circuit"]
        BH --> PFC["PathFilterChatHandler"]
        PFC -->|1-byte path (if blocked)| SKIP
        PFC --> WH["WelcomeChatHandler"]
        WH --> PU["PathUpgradeChatHandler"]
        PU --> CMD["CommandChatHandler"]
        CMD --> AIH["AiChatHandler"]
        AIH --> AIS["AI Chat Service (Spring AI)"]
        AIS -->|Tools| T1["getWeather"]
        CS --> REPO["Repositories (JPA + SQLite)"]
        REPO --> CM["ChatMessage"]
        REPO --> CHAN["ChatChannel"]
        REPO --> MEM["ChatMemory"]
        REPO --> CP["ChatParticipant"]
        CS --> MS["MemoryService (scheduled)"]
    end
```

**Handler chain** — larger `getOrder()` runs first; `AiChatHandler` is the last resort.

## Prerequisites

- **Docker** — for containerized deployment
- **OpenAI-compatible AI endpoint** — e.g. [Ollama](https://ollama.com/), llama.cpp, or any OpenAI-compatible API
- **JDK 25** — for local development

## Quick Start

### 1. Build and run with Docker Compose

```bash
# Edit docker-compose.yml with your AI endpoint settings
vim docker-compose.yml

# Build the image
docker compose build

# Start
docker compose up -d
```

### 2. Configure RemoteTerm

Copy `bot.py` to your RemoteTerm bot directory and set the environment variable:

```bash
export NCBOT_URL=http://localhost:8080/v1/chat
```

RemoteTerm will invoke `bot(**kwargs)` for every message. The script forwards it to ncbot and returns the AI response.

### 3. Access the Admin API

Browse entities via the custom admin API at `http://localhost:8080/v1/channels`.

## Configuration

Runtime configuration is stored in the **database** (`config_item` table) and managed through
the admin UI (**Settings** page at `/v1/config`) or the `/v1/config` API. Every item is defined in
`ConfigItemDefinition` and seeded with a sensible default on first start. Values apply immediately
(except items flagged as *restart required* — currently `chat.max-reply-tokens`).

Only secrets, infrastructure, and values consumed solely at startup remain in `application.yml`
/env vars:

| Variable | Default | Description |
|---|---|---|
| `NCBOT_API_KEY` | `default-key` | API key for the OpenAI-compatible endpoint |
| `NCBOT_OPENAI_BASE_URL` | *(from application.yml)* | Base URL for the AI server |
| `NCBOT_MODEL` | `ncbot` | Model name/identifier |
| `NCBOT_MEMORY_UPDATE_PERIOD` | `30m` | Scheduled interval for AI memory synthesis (restart to change) |
| `NCBOT_AI_TIMEOUT` | `10m` | Max time for the AI model to answer (applies to the OpenAI SDK client) |
| `NCBOT_WEATHER_TIMEOUT` | `10s` | Max time for weather tool lookups |
| `reasoning-effort` (`spring.ai.openai.chat.options.reasoning-effort`) | `low` | Reasoning effort; model-dependent (deepseek-v4-flash: `max`/`high`/`low`). Env `SPRING_AI_OPENAI_CHAT_OPTIONS_REASONING_EFFORT` |

Previously `NCBOT_*`-configurable items (`NCBOT_NAME`, `NCBOT_MAX_REPLY_BYTES`,
`NCBOT_AI_ENABLED`, `NCBOT_CHANNELS_*`, `NCBOT_BLOCK_*`, prompts, etc.) now live in the DB — set
them via the Settings UI instead of environment variables.

Bot-script (`bot.py`) options follow in the Deployment section.

### Database-Backed Configuration

The `config_item` table is a key–value store keyed by dot-separated names, seeded and managed by
`ConfigService`:

| Namespace | Example keys | Type |
|---|---|---|
| `bot.*` | `bot.name`, `bot.system-prompt`, `bot.condense-prompt`, `bot.memory-prompt` | STRING / TEXT |
| `chat.*` | `chat.welcome-content`, `chat.max-history`, `chat.ai-enabled`, `chat.auto-update-memory`, `chat.use-memory`, `chat.condense`, `chat.allow-one-byte-paths`, `chat.path-upgrade-cooldown-minutes`, `chat.minimum-response-ms`, `chat.max-reply-bytes`, `chat.max-reply-tokens` | BOOLEAN / INT / LONG |
| `memory.*` | `memory.partition-size`, `memory.max-failures` | INT |
| `channels.*` | `channels.welcome`, `channels.command`, `channels.path-upgrade`, `channels.ai-each`, `channels.ai-tagged`, `channels.allowed-dms` | STRING / LIST |
| `blocking.*` | `blocking.block-user`, `blocking.allow-user`, `blocking.block-path`, `blocking.allow-path`, `blocking.block-channel`, `blocking.allow-channel` | STRING |

- **GET `/v1/config`** — every item with type, current/default value, description and restart flag
- **PUT `/v1/config/{key}`** — update (validated by type; invalid → `400`)
- **DELETE `/v1/config/{key}`** — reset to default

### Channel Configuration (Regex Patterns)

Channels are configured via regex patterns stored in the DB (`channels.*` keys) — one per
capability, matched against the channel name:

| Key | Default |
|---|---|
| `channels.welcome` | `^#ncbot$` |
| `channels.command` | `^#ncbot$` |
| `channels.path-upgrade` | `^#ncbot$` |
| `channels.ai-each` | `^#ncbot$` |
| `channels.ai-tagged` | `.*` |

Channels are configured via regex patterns — one per capability. Each pattern is matched against the channel name:

```yaml
ncbot:
  channels-welcome: "^#ncbot$"
  channels-command: "^#ncbot$"
  channels-path-upgrade: "^#ncbot$"
  channels-ai-each: "^#ncbot$"
  channels-ai-tagged: ".*"
```

**AI Mode Resolution:**
1. If `ai-enabled` is `false`, all channels default to `DISABLED`
2. If the channel matches `channels-ai-each`, mode is `EACH` (respond to every message)
3. If the channel matches `channels-ai-tagged`, mode is `TAGGED` (respond only when mentioned)
4. Default is `DISABLED` if the channel matches neither AI list

**Other flags** (`welcome`, `command`, `path-upgrade`) are independent boolean flags — presence in the list means `true`, absence means `false`.

Set these keys in the DB via the Settings UI (or `PUT /v1/config/{key}`); e.g.
`channels.ai-each = "^#ncbot$"`, `channels.ai-tagged = ".*"`.

### User/Path Blocking

Rules are DB settings under `blocking.*` (e.g. `blocking.block-user = ".*(bot|spam|scam).*"`,
`blocking.allow-user = "admin.*"`, `blocking.block-path`, `blocking.allow-path`), edited via the
Settings UI.

**Precedence:** allow always beats block. If a user/path matches an allow pattern, they are allowed regardless of block patterns.

### Per-Participant Blocking

In addition to the `block-user` regex, individual participants can be blocked directly via the admin API or the frontend. Each participant has a `blocked` flag (`chat_participant.blocked`) that can be toggled with `PUT /v1/participants/{participantId}` and body `{ "blocked": true | false }`. The frontend shows a Block/Unblock button on the Participants page and in each channel's Participants tab.

A blocked participant behaves like a `block-user` regex match — the bot ignores their messages (an `allow-user` regex still takes precedence). Because the flag is stored in the database, it survives restarts and regex config changes.

### DM Access Control

DMs are controlled via a comma-separated list of allowed sender keys. Set the
`channels.allowed-dms` DB setting (e.g. `"hex-key-1, hex-key-2"`) in the Settings UI.

Leave empty or unset to block all DMs. DMs always have `ai: EACH`, `welcome: true`, and `command: true`.

## RemoteTerm Setup

### Bot Script

The `bot.py` file is the RemoteTerm integration script. It runs **async fire-and-forget** so that AI latency is never limited by RemoteTerm's 10-second bot execution timeout (`BOT_EXECUTION_TIMEOUT`):

1. Receives kwargs from RemoteTerm's bot system and returns `None` immediately (well under the 10 s budget)
2. In a background daemon thread, POSTs the message to ncbot's `/v1/chat` endpoint with a long timeout (default 30 min)
3. When the reply arrives, POSTs it to RemoteTerm's *own* HTTP API (`/api/messages/direct` for DMs, `/api/messages/channel` for channels) so it goes out on the mesh

Key behaviors:
- **No reply to own messages** — skips messages where `is_outgoing` is true
- **Configurable AI latency ceiling** — the background thread can wait as long as needed; the only bound is ncbot's own AI client timeout, `NCBOT_AI_TIMEOUT` (default 10 min)
- **Best-effort delivery** — replies are dropped (with a log line) if RemoteTerm's API is unreachable or RemoteTerm restarts mid-call; no retries
- **Send spacing enforced** — reimplements RemoteTerm's 2 s `BOT_MESSAGE_SPACING` between bot sends so repeaters can return to listening mode
- **Graceful failure** — returns `None` on any error
- **No dependencies** — uses only Python standard library

All knobs are env vars of the RemoteTerm process (see the configuration table below): `NCBOT_TIMEOUT`, `NCBOT_MAX_PENDING`, `RT_API_URL`, `RT_API_TIMEOUT`, `NCBOT_MESSAGE_SPACING`. RemoteTerm's optional HTTP Basic auth (`MESHCORE_BASIC_AUTH_USERNAME`/`MESHCORE_BASIC_AUTH_PASSWORD`) is picked up automatically.

### Bot Kwargs

RemoteTerm passes these kwargs:

| Parameter | Description |
|-----------|-------------|
| `sender_name` | Display name of sender (nullable) |
| `sender_key` | Hex public key (nullable for channels) |
| `message_text` | The message content |
| `is_dm` | True for DMs, false for channels |
| `channel_key` | Hex channel key (nullable for DMs) |
| `channel_name` | Channel name with hash (nullable for DMs) |
| `sender_timestamp` | Unix seconds (nullable) |
| `path` | Hex-encoded routing path (nullable) |
| `is_outgoing` | Whether this is our own outgoing message |
| `path_bytes_per_hop` | 1, 2, or 3 (nullable) |

## AI Features

### Memory System

ncbot maintains long-term memory per channel using AI-generated key-value pairs. A scheduled task periodically synthesizes conversation history into dense factual records (e.g., `user.john.pref.color=blue`). The memory is included in AI prompts to provide context across sessions.

- **Condensing**: When an AI response exceeds the byte limit, a second AI call condenses it to fit
- **Partitions**: Memory updates process messages in configurable batches
- **Storage**: Memories are stored in the `chat_memory` table, scoped to each channel; global memories (`chat_channel_id = NULL`) apply everywhere
- **Tool-driven**: Memory synthesis no longer parses model text output. The model updates memory directly via the `insertMemory`/`updateMemory`/`deleteMemory` tools (channel-scoped only; global memory is read-only). Current memories are supplied inline as `__CHAT_MEMORY__`, so no read tools are needed. The same tools are available in live chat, so durable facts can be stored in real time
- **Resilience**: Each channel's synthesis is run independently, so a transient AI failure (e.g. a provider timeout/stream reset) on one channel doesn't abort updates for the others; the memory-synthesis AI call itself retries up to 3 times per partition. After `memory.max-failures` consecutive failed runs, a partition is skipped (cursor advanced past it) and recorded via `/v1/memories/failures`, so a deterministically failing batch is never retried forever

### Tools

The AI model has access to these tools:

| Tool | Description |
|------|-------------|
| `getWeather` | Get current weather **and** the 7-day forecast for a location (via Open-Meteo). Returns location/time zone, current observations (temperature, feels-like, wind speed, gusts, wind direction, humidity, precipitation, cloud cover, pressure, conditions) and a per-day forecast (high/low, conditions, precipitation chance, UV index, sunrise/sunset). Bounded by `NCBOT_WEATHER_TIMEOUT` (default 10 s) so a hung lookup can't stall the AI call. |
| `insertMemory(channelId, key, value)` | Add a new channel memory; fails if the key already exists. A channel memory with the same key as a global one overrides it for that channel. |
| `updateMemory(channelId, key, value)` | Update an existing channel memory; fails if it does not exist. |
| `deleteMemory(channelId, key)` | Delete an existing channel memory; fails if it does not exist. |

The tool/param descriptions tell the model to estimate coordinates from a location name and to judge freshness: chat history is a JSON array (`__CHAT_MESSAGES__`) where each message carries an absolute `timestamp` and a coarse human-readable `age` (e.g. `"age":"2h ago"`), rendered alongside the current `Time:` line, so a time-sensitive claim like weather older than ~15 minutes is treated as stale and triggers a fresh tool call, while fresh data avoids one.

## Admin API

Custom endpoints at `/v1/*` provide read access to all entities plus full CRUD on memories. Global and channel-specific memory operations use **separate, distinct routes** (no optional channel parameters).

All read endpoints support pagination via `?page=0&size=25` (0-indexed page, default 25 per page).
The channels, memory, and participants list endpoints also accept `?query=` for a
case-insensitive substring search (channels: name/key, memories: key/value, participants: name).
Responses use the generic `PageResponse<T>` wrapper:

```json
{
  "content": [...],
  "totalPages": 5,
  "currentPage": 0,
  "totalElements": 123
}
```

| Path | Method | Description |
|------|--------|-------------|
| `/v1/channels` | GET | All channels (filter: `?dm=true\|false`, search: `?query=`) — `PageResponse<ChannelDto>`, sorted by last message descending |
| `/v1/channels/{channelId}/messages` | GET | Messages (`?page`, `?size`, `?before=ISO-instant`, `?after=ISO-instant`, `?sortDirection=ASC\|DESC`) — `MessagesResponse` |
| `/v1/channels/{channelId}/memory` | GET | Channel-specific memories (search: `?query=`) — `PageResponse<MemoryDto>` |
| `/v1/channels/{channelId}/memory` | POST | Create channel memory (body: `{key, value}`) — `MemoryDto` |
| `/v1/channels/{channelId}/memory/{id}` | PUT | Update channel memory (body: `{key, value}`) — validates channel match — `MemoryDto` |
| `/v1/channels/{channelId}/memory/{id}` | DELETE | Delete channel memory — validates channel match — `204 No Content` |
| `/v1/channels/{channelId}/memory/{id}/promote` | POST | Promote channel memory to global (deletes source) — `MemoryDto` |
| `/v1/channels/{channelId}/participants` | GET | Participants for a channel (search: `?query=`) — `PageResponse<ParticipantDto>` |
| `/v1/memory` | GET | Global memories (search: `?query=`) — `PageResponse<MemoryDto>` |
| `/v1/memory` | POST | Create global memory (body: `{key, value}`) — `MemoryDto` |
| `/v1/memory/{id}` | PUT | Update global memory (body: `{key, value}`) — validates global scope — `MemoryDto` |
| `/v1/memory/{id}` | DELETE | Delete global memory — validates global scope — `204 No Content` |
| `/v1/memories/failures` | GET | Memory-synthesis partitions skipped after repeated AI failures — `PageResponse<MemoryFailureDto>` |
| `/v1/channels/{channelId}/memories/failures` | GET | Memory-synthesis failures for one channel — `PageResponse<MemoryFailureDto>` |
| `/v1/memories/failures/{id}/retry` | POST | Rewind the channel cursor and clear the record so a skipped batch is re-processed — `204 No Content` |
| `/v1/participants` | GET | All participants with last seen (search: `?query=`) — `PageResponse<ParticipantDto>` |
| `/v1/participants/{participantId}` | PUT | Block/unblock a participant (body: `{blocked: true\|false}`) — `ParticipantDto` |

**Validation rules:**
- Channel memory endpoints (`/v1/channels/{channelId}/memory/*`) reject requests where the memory's `chatChannelId` doesn't match the path parameter
- Global memory endpoints (`/v1/memory/*`) reject requests where the memory has a non-null `chatChannelId`
- Promote endpoint validates source belongs to the specified channel, then copies to global and deletes the source

## Commands

Commands are per-channel (controlled by the `channels-command` list). They are matched case-insensitively:

| Command | Aliases | Response |
|---------|---------|----------|
| `help` | `h` | List of available commands |
| `ping` | `p` | "pong" |
| `path` | `m`, `multipath`, `multitest` | Hex-encoded routing path decoded into hops |
| `test` | `t` | Connection info (time, path) |
| `users` | `u`, `user` | List of known users |
| `channels` | `c`, `channel` | List of known channels |
| `dice` | — | Roll a die: `d<sides>` (e.g., `d20`) |
| `random` | `r`, `rand` | Random float between 0 and 1 |

## Database

SQLite database file lives at `/data/ncbot.db` inside the container. The `docker-compose.yml` mounts `./data:/data` by default.

- **Persisting data:** Keep the volume mount in docker-compose.yml
- **Ephemeral storage:** Remove the volume mount
- **Upgrading:** `docker compose pull && docker compose up -d` — DB persists across upgrades

## Troubleshooting

### AI Connection Failed

- Verify `NCBOT_OPENAI_BASE_URL` points to a running AI server
- Check that `NCBOT_API_KEY` is correct (if required by your server)
- Test with: `curl -v $NCBOT_OPENAI_BASE_URL/v1/models`

### Bot Not Responding

- Check ncbot logs: `docker compose logs ncbot`
- Verify channel configuration — the channel must appear in `channels-ai-each` or `channels-ai-tagged`
- For DMs, check that the sender key is in `ncbot.allowed-dms`
- Look for filter log messages: "skipping channel" or "skipping DM"

### Responses Too Long

- Reduce `chat.max-reply-bytes` (default 128) in the Settings UI
- The system prompt instructs the AI to keep responses under the limit
- Condensing is enabled by default — a second AI call will compress oversized responses

### Slow Responses

The bot is async, so latency = RemoteTerm's 2 s settle delay + ncbot processing + mesh send. To speed up ncbot processing:

- `reasoning-effort: low` is set by default (deepseek-v4-flash's fastest supported effort — the model default is `high`)
- `chat.max-reply-tokens` (default 256) caps output generation (restart required)
- Reduce `chat.max-history` (default 25) in Settings — fewer input tokens means faster prefill and lower cost (matters on OpenRouter)
- On OpenRouter, pick a fast non-reasoning model — model choice is the single biggest speed lever
- OpenRouter-only (YAML `extra-body`): `provider.sort: throughput` routes to the fastest provider; `transforms: ["middle-out"]` roughly halves output tokens
- `NCBOT_AI_TIMEOUT` (default 10 min) caps how long a call may take; lower it to fail fast, but keep it generous — OpenRouter free-tier can queue
- Each tool call adds a full extra OpenRouter round-trip; the system prompt already discourages unnecessary ones

### User Blocked Unexpectedly

- Check `blocking.block-user` (Settings) — the user name may match a regex
- Check the participant's `blocked` flag — it may have been blocked via the admin API/frontend
- Use `blocking.allow-user` (Settings) to whitelist specific users
- Check logs for "blocked" messages

### 1-Byte Path Messages Not Responding

- 1-byte paths are **allowed by default** (`chat.allow-one-byte-paths = true`). If set to `false`, `PathFilterChatHandler` blocks them from reaching command/AI handlers.
- Welcome and path-upgrade notifications still work for blocked paths.
- Check the `chat.allow-one-byte-paths` setting (Settings UI) if 1-byte path messages are unexpectedly blocked.

## Upgrading

```bash
docker compose pull
docker compose up -d
```

## Development

```bash
# Run locally (requires JDK 25)
./gradlew bootRun

# Build
./gradlew build

# Run tests
./gradlew test
```

## License

See LICENSE file
