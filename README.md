# ncbot

AI chat provider for [Meshcore](https://meshcore.net/), invoked by the RemoteTerm application for every message sent to a chat channel or DM thread. It persists messages to SQLite, runs them through an OpenAI-compatible AI model, and returns short responses (≤ 128 UTF-8 bytes) that RemoteTerm delivers back to the mesh network.

## Architecture

```
RemoteTerm (Python bot)
       │ POST /v1/chat
       ▼
┌──────────────────────────────────────┐
│           ncbot (Spring Boot)         │
│                                       │
│  ChatController → ChatService         │
│       │                               │
│  ChatHandler chain (ordered):         │
│    • WelcomeChatHandler (new users)   │
│    • CommandChatHandler (commands)    │
│    • AiChatHandler (AI responses)     │
│       │                               │
│  Repositories (JPA + SQLite):         │
│    • ChatMessage  • ChatChannel       │
│    • ChatMemory   • ChatParticipant   │
│       │                               │
│  AI Chat Service (Spring AI)          │
│       │                               │
│  Tools: Weather, ChatInfo             │
│       │                               │
│  MemoryService (scheduled condense)   │
│       │                               │
│  Admin UI (jte templates + MVC)       │
└──────────────────────────────────────┘
```

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

### 3. Access the Admin UI

Open `http://localhost:8080/admin` in your browser.

## Configuration

All configuration is via environment variables or `application.yaml`:

| Variable | Default | Description |
|----------|---------|-------------|
| `NCBOT_API_KEY` | `default-key` | API key for the OpenAI-compatible endpoint |
| `NCBOT_OPENAI_BASE_URL` | *(from application.yaml)* | Base URL for the AI server |
| `NCBOT_MODEL` | `ncbot` | Model name/identifier |
| `NCBOT_MINIMUM_RESPONSE_MS` | `3000` | Minimum response delay in milliseconds (0 to disable) |
| `NCBOT_MAX_REPLY_BYTES` | `128` | Max UTF-8 bytes per reply message |
| `NCBOT_CONDENSE` | `true` | Enable AI-based response condensing when over byte limit |
| `NCBOT_MEMORY_UPDATE_PERIOD` | `30m` | Scheduled interval for AI memory synthesis |
| `NCBOT_MEMORY_PARTITION_SIZE` | `100` | Number of messages per memory partition |
| `NCBOT_WELCOME_CONTENT` | *(empty)* | Welcome message appended for new participants |

### Channel Configuration

Channels are configured in `application.yaml` with per-channel flags:

```yaml
ncbot:
  channels:
    - name: "#ncbot"
      ai: true        # Enable AI-generated responses
      welcome: true   # Send welcome message to new participants
      command: true   # Enable command handler (help, ping, etc.)
```

- **`ai`** — Whether the channel gets AI-generated responses
- **`welcome`** — Whether new participants receive a welcome message
- **`command`** — Whether command shortcuts are active in this channel

### DM Access Control

DMs are controlled via a set of allowed sender keys:

```yaml
ncbot:
  allowed-dms:
    - "hex-key-1"
    - "hex-key-2"
```

Leave empty or unset to block all DMs. DMs always have `ai: true`, `welcome: true`, and `command: true` enabled.

## RemoteTerm Setup

### Bot Script

The `bot.py` file is the RemoteTerm integration script. It:

1. Receives kwargs from RemoteTerm's bot system
2. POSTs to ncbot's `/v1/chat` endpoint
3. Returns the response (list of strings, or None)

Key behaviors:
- **No reply to own messages** — skips messages where `is_outgoing` is true
- **9-second timeout** — RemoteTerm allows 10 seconds total, leaving 1s margin
- **Graceful failure** — returns `None` on any error (bot silently fails)
- **No dependencies** — uses only Python standard library

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
| `getCurrentWeather` | Get current weather by latitude/longitude (via Open-Meteo). Returns temperature (°F), wind speed (mph), wind direction (°), humidity (%), and conditions. |
| `getKnownChannels` | List all known Meshcore channels the bot has seen |
| `searchUsers` | Search for users by name substring |

## Admin UI

Access at `http://host:8080/admin`:

| Route | Purpose |
|-------|---------|
| `/admin` | Dashboard — message stats, uptime, channel stats |
| `/admin/messages` | Message log viewer (paginated) |
| `/admin/config` | View current configuration |

## Commands

Commands are per-channel (controlled by the `command` flag). They are matched case-insensitively and use single-letter aliases:

| Command | Alias | Response |
|---------|-------|----------|
| `help` | `h` | List of available commands |
| `ping` | `p` | "pong" |
| `path` | `m` | Hex-encoded routing path decoded into hops |
| `test` | `t` | Connection info (time, path) |
| `users` | `u` | List of known users |
| `channels` | `c` | List of known channels |

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
- Verify channel configuration — the channel must be listed in `ncbot.channels` with `ai: true`
- For DMs, check that the sender key is in `ncbot.allowed-dms`
- Look for filter log messages: "skipping channel" or "skipping DM"

### Responses Too Long

- Reduce `NCBOT_MAX_REPLY_BYTES` (default 128)
- The system prompt instructs the AI to keep responses under the limit
- Condensing is enabled by default — a second AI call will compress oversized responses

### Slow Responses

- Reduce `NCBOT_MINIMUM_RESPONSE_MS` (default 3000)
- Check AI model inference speed
- Consider a faster model or hardware acceleration

## Commands

ncbot is AI-driven rather than command-driven, but recognizes these intents:

| Command | Response |
|---------|----------|
| `test` / `t` | Connection info from message details |
| `ping` / `p` | "pong" |
| `path` / `m` | Hex-encoded routing path |
| `help` / `h` | List of available capabilities |
| `users` / `u` | List of known users |
| `channels` / `c` | List of known channels |

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

See LICENSE file.
