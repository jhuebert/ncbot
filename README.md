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
│  ChatEndpoint → ChatService           │
│       │                               │
│  Message Repository (JPA + SQLite)    │
│       │                               │
│  AI Chat Service (Spring AI)          │
│       │                               │
│  Tools: Weather, History, ByteCount   │
│                                       │
│  Admin UI (jte templates + MVC)       │
└──────────────────────────────────────┘
```

## Prerequisites

- **Docker** — for containerized deployment
- **OpenAI-compatible AI endpoint** — e.g. [llama.cpp](https://github.com/ggerganov/llama.cpp) server, Ollama, or any OpenAI-compatible API

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
| `NCBOT_OPENAI_BASE_URL` | `http://localhost:8080` | Base URL for llama.cpp or other OpenAI-compatible server |
| `NCBOT_MODEL` | `llama3` | Model name/identifier |
| `NCBOT_TEMPERATURE` | `0.7` | AI temperature (0.0–1.0) |
| `NCBOT_RESPONSE_DELAY_SECONDS` | `1.5` | Delay before AI processing (0 to disable) |
| `NCBOT_MAX_REPLY_BYTES` | `128` | Max UTF-8 bytes per reply message |
| `NCBOT_HISTORY_LIMIT` | `20` | Number of recent messages to include in AI context |
| `NCBOT_SYSTEM_PROMPT` | *(built-in default)* | Optional override for the system prompt |
| `NCBOT_ALLOW_DMS` | `false` | Whether the bot responds to direct messages |

### Channel Filtering

To restrict the bot to specific channels, set allowed channel keys in `application.yaml`:

```yaml
ncbot:
  allowed-channels:
    - "abc123def456"
    - "789xyz000111"
```

Leave empty or unset to respond in all channels.

## RemoteTerm Setup

### Bot Script

The `bot.py` file is the RemoteTerm integration script. It:

1. Receives kwargs from RemoteTerm's bot system
2. POSTs to ncbot's `/v1/chat` endpoint
3. Returns the response (string, list of strings, or None)

Key behaviors:
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

## Database

SQLite database file lives at `/data/ncbot.db` inside the container. The `docker-compose.yml` mounts `./data:/data` by default.

- **Persisting data:** Keep the volume mount in docker-compose.yml
- **Ephemeral storage:** Remove the volume mount
- **Upgrading:** `docker compose pull && docker compose up -d` — DB persists across upgrades

## Admin UI

Access at `http://host:8080/admin`:

| Route | Purpose |
|-------|---------|
| `/admin` | Dashboard — message stats, uptime, channel stats |
| `/admin/messages` | Message log viewer (paginated) |
| `/admin/config` | View current configuration |

## Troubleshooting

### AI Connection Failed

- Verify `NCBOT_OPENAI_BASE_URL` points to a running AI server
- Check that `NCBOT_API_KEY` is correct (if required by your server)
- Test with: `curl -v $NCBOT_OPENAI_BASE_URL/v1/models`

### Bot Not Responding

- Check ncbot logs: `docker compose logs ncbot`
- Verify channel filtering isn't blocking your channel
- Check `allow-dms` setting for DMs
- Look for filter log messages: "Skipping channel" or "Skipping DM"

### Responses Too Long

- Reduce `NCBOT_MAX_REPLY_BYTES` (default 128)
- The system prompt instructs the AI to keep responses under the limit
- The bot enforces the limit by truncating/splitting at sentence boundaries

### Slow Responses

- Reduce `NCBOT_RESPONSE_DELAY_SECONDS` (default 1.5)
- Check AI model inference speed
- Consider a faster model or hardware acceleration

## Commands

ncbot is AI-driven rather than command-driven, but recognizes these intents:

| Command | Response |
|---------|----------|
| `test` / `t` | Connection info from message details |
| `ping` | "pong" |
| `path` / `decode` / `route` | Hex-encoded routing path |
| `help` | List of available capabilities |
| `hello` / `hi` / `hey` | Friendly greeting |
| `weather [location]` / `gwx` | Current weather (via Open-Meteo) |

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
