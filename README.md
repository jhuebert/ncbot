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
        AIS -->|Tools| T1["getCurrentWeather"]
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

All configuration is via environment variables or `application.yml`:

| Variable                              | Default                  | Description                                                         |
|---------------------------------------|--------------------------|---------------------------------------------------------------------|
| `NCBOT_API_KEY`                       | `default-key`            | API key for the OpenAI-compatible endpoint                          |
| `NCBOT_OPENAI_BASE_URL`               | *(from application.yml)* | Base URL for the AI server                                          |
| `NCBOT_MODEL`                         | `ncbot`                  | Model name/identifier                                               |
| `NCBOT_NAME`                          | `ncbot`                  | Bot display name                                                    |
| `NCBOT_MINIMUM_RESPONSE_MS`           | `0`                      | Minimum response delay in milliseconds (0 = disabled; pacing handled by RemoteTerm's settle delay + send spacing) |
| `NCBOT_MAX_REPLY_BYTES`               | `128`                    | Max UTF-8 bytes per reply message                                   |
| `NCBOT_CONDENSE`                      | `true`                   | Enable AI-based response condensing when over byte limit            |
| `NCBOT_MEMORY_UPDATE_PERIOD`          | `30m`                    | Scheduled interval for AI memory synthesis                          |
| `NCBOT_MEMORY_PARTITION_SIZE`         | `100`                    | Number of messages per memory partition                             |
| `NCBOT_MAX_CHAT_HISTORY`              | `25`                     | Number of recent messages to include in AI prompts                  |
| `NCBOT_AI_ENABLED`                    | `true`                   | Master switch for AI (when false, all channels default to DISABLED) |
| `NCBOT_AUTO_UPDATE_MEMORY`            | `true`                   | Enable scheduled memory synthesis                                   |
| `NCBOT_USE_MEMORY`                    | `true`                   | Include memories in AI prompts                                      |
| `NCBOT_ALLOW_ONE_BYTE_PATHS`          | `true`                   | Allow 1-byte path messages through the filter                       |
| `NCBOT_PATH_UPGRADE_COOLDOWN_MINUTES` | `1440`                   | Cooldown between path upgrade notifications                         |
| `NCBOT_CHANNELS_WELCOME`              | `^#ncbot$`               | Regex pattern for channels that receive welcome messages            |
| `NCBOT_CHANNELS_COMMAND`              | `^#ncbot$`               | Regex pattern for channels that accept commands                     |
| `NCBOT_CHANNELS_PATH_UPGRADE`         | `^#ncbot$`               | Regex pattern for path upgrade notifications                        |
| `NCBOT_CHANNELS_AI_EACH`              | `^#ncbot$`               | Regex pattern for channels where AI responds to every message       |
| `NCBOT_CHANNELS_AI_TAGGED`            | `.*`                     | Regex pattern for channels where AI responds only when mentioned    |
| `NCBOT_ALLOWED_DMS`                   | *(empty)*                | Comma-separated list of allowed DM sender hex keys                  |
| `NCBOT_BLOCK_USER`                    | *(empty)*                | Regex pattern to block users by name                                |
| `NCBOT_ALLOW_USER`                    | *(empty)*                | Regex pattern to allow users (overrides block)                      |
| `NCBOT_BLOCK_PATH`                    | *(empty)*                | Regex pattern to block paths                                        |
| `NCBOT_ALLOW_PATH`                    | *(empty)*                | Regex pattern to allow paths (overrides block)                      |
| `NCBOT_WELCOME_CONTENT`               | *(empty)*                | Custom welcome message content                                      |
| `NCBOT_TIMEOUT`                       | `1800`                   | Seconds to wait for ncbot's `/v1/chat` response (bot script)        |
| `NCBOT_MAX_PENDING`                   | `50`                     | Max in-flight AI requests; beyond this, new messages are dropped    |
| `RT_API_URL`                          | `http://localhost:8000/api` | RemoteTerm HTTP API base URL for delivering replies (bot script)  |
| `RT_API_TIMEOUT`                      | `30`                     | Seconds per RemoteTerm API delivery call (bot script)               |
| `NCBOT_MESSAGE_SPACING`               | `2.0`                    | Min seconds between bot sends (bot script; reimplements RemoteTerm's spacing) |
| `NCBOT_AI_TIMEOUT`                    | `10m`                    | Max time for the AI model to answer (applies to the OpenAI SDK client)      |
| `NCBOT_WEATHER_TIMEOUT`               | `10s`                    | Max time for weather tool lookups (prevents hung tool calls from stalling AI) |
| `NCBOT_MAX_REPLY_TOKENS`               | `256`                    | Cap on output tokens per AI round-trip on the reply path (chat, tool-call args, condense). Memory synthesis is not capped |
| `reasoning-effort` (yaml `spring.ai.openai.chat.options.reasoning-effort`) | `low` | Reasoning effort; model-dependent values (deepseek-v4-flash: `max`/`high`/`low`, default `high` — `low` is fastest; `none` is invalid for it). Env `SPRING_AI_OPENAI_CHAT_OPTIONS_REASONING_EFFORT` |
| `NCBOT_SYSTEM_PROMPT`                 | *(from application.yml)* | System prompt for AI                                                |
| `NCBOT_CONDENSE_PROMPT`               | *(from application.yml)* | Prompt for response condensing                                      |
| `NCBOT_MEMORY_PROMPT`                 | *(from application.yml)* | Prompt for memory synthesis                                         |

### Channel Configuration (Regex Patterns)

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

**Environment variable example:**
```bash
NCBOT_CHANNELS_AI_EACH="^#ncbot$"
NCBOT_CHANNELS_AI_TAGGED=".*"
```

### User/Path Blocking

```yaml
ncbot:
  block-user: ".*(bot|spam|scam).*"
  allow-user: "admin.*"
  block-path: ".*malicious.*"
  allow-path: "internal.*"
```

**Precedence:** allow always beats block. If a user/path matches an allow pattern, they are allowed regardless of block patterns.

### DM Access Control

DMs are controlled via a comma-separated list of allowed sender keys:

```yaml
ncbot:
  allowed-dms: "hex-key-1, hex-key-2"
```

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
- **Storage**: Memories are stored in the `chat_memory` table, scoped to each channel

### Tools

The AI model has access to these tools:

| Tool | Description |
|------|-------------|
| `getCurrentWeather` | Get current weather by latitude/longitude (via Open-Meteo). Returns temperature (°F), wind speed (mph), wind direction (°), humidity (%), and conditions. Bounded by `NCBOT_WEATHER_TIMEOUT` (default 10 s) so a hung lookup can't stall the AI call. |

The tool/param descriptions tell the model to estimate coordinates from a location name and to judge freshness: chat history carries per-message ages (e.g. `[2h ago]`, rendered in `__CHAT_MESSAGES__` alongside the current `Time:` line), so a time-sensitive claim like weather older than ~15 minutes is treated as stale and triggers a fresh tool call, while fresh data avoids one.

## Admin API

Custom endpoints at `/v1/*` provide read access to all entities plus full CRUD on memories. Global and channel-specific memory operations use **separate, distinct routes** (no optional channel parameters).

All read endpoints support pagination via `?page=0&size=25` (0-indexed page, default 25 per page). Responses use the generic `PageResponse<T>` wrapper:

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
| `/v1/channels` | GET | All channels (filter: `?dm=true\|false`) — `PageResponse<ChannelDto>` |
| `/v1/channels/{channelId}/messages` | GET | Messages (`?page`, `?size`, `?before=ISO-instant`, `?after=ISO-instant`, `?sortDirection=ASC\|DESC`) — `MessagesResponse` |
| `/v1/channels/{channelId}/memory` | GET | Channel-specific memories — `PageResponse<MemoryDto>` |
| `/v1/channels/{channelId}/memory` | POST | Create channel memory (body: `{key, value}`) — `MemoryDto` |
| `/v1/channels/{channelId}/memory/{id}` | PUT | Update channel memory (body: `{key, value}`) — validates channel match — `MemoryDto` |
| `/v1/channels/{channelId}/memory/{id}` | DELETE | Delete channel memory — validates channel match — `204 No Content` |
| `/v1/channels/{channelId}/memory/{id}/promote` | POST | Promote channel memory to global (deletes source) — `MemoryDto` |
| `/v1/channels/{channelId}/participants` | GET | Participants for a channel — `PageResponse<ParticipantDto>` |
| `/v1/memory` | GET | Global memories — `PageResponse<MemoryDto>` |
| `/v1/memory` | POST | Create global memory (body: `{key, value}`) — `MemoryDto` |
| `/v1/memory/{id}` | PUT | Update global memory (body: `{key, value}`) — validates global scope — `MemoryDto` |
| `/v1/memory/{id}` | DELETE | Delete global memory — validates global scope — `204 No Content` |
| `/v1/participants` | GET | All participants with last seen — `PageResponse<ParticipantDto>` |

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

- Reduce `NCBOT_MAX_REPLY_BYTES` (default 128)
- The system prompt instructs the AI to keep responses under the limit
- Condensing is enabled by default — a second AI call will compress oversized responses

### Slow Responses

The bot is async, so latency = RemoteTerm's 2 s settle delay + ncbot processing + mesh send. To speed up ncbot processing:

- `reasoning-effort: low` is set by default (deepseek-v4-flash's fastest supported effort — the model default is `high`)
- `NCBOT_MAX_REPLY_TOKENS` (default 256) caps output generation
- Reduce `NCBOT_MAX_CHAT_HISTORY` (default 25) — fewer input tokens means faster prefill and lower cost (matters on OpenRouter)
- On OpenRouter, pick a fast non-reasoning model — model choice is the single biggest speed lever
- OpenRouter-only (YAML `extra-body`): `provider.sort: throughput` routes to the fastest provider; `transforms: ["middle-out"]` roughly halves output tokens
- `NCBOT_AI_TIMEOUT` (default 10 min) caps how long a call may take; lower it to fail fast, but keep it generous — OpenRouter free-tier can queue
- Each tool call adds a full extra OpenRouter round-trip; the system prompt already discourages unnecessary ones

### User Blocked Unexpectedly

- Check `block-user` — the user name may match a regex
- Use `allow-user` to whitelist specific users
- Check logs for "blocked" messages

### 1-Byte Path Messages Not Responding

- 1-byte paths are **allowed by default** (`NCBOT_ALLOW_ONE_BYTE_PATHS=true`). If set to `false`, `PathFilterChatHandler` blocks them from reaching command/AI handlers.
- Welcome and path-upgrade notifications still work for blocked paths.
- Check `NCBOT_ALLOW_ONE_BYTE_PATHS` setting if 1-byte path messages are unexpectedly blocked.

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
