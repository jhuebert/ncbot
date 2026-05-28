# ncbot — Agent Guide

**AI chat bot for [Meshcore](https://meshcore.net/)** — receives messages via HTTP, runs them through an OpenAI-compatible model, and returns short responses (≤ 128 UTF-8 bytes).

## TL;DR

- **What it does:** RemoteTerm (Python bot) forwards every chat message to `POST /v1/chat` → ncbot processes it through a chain of handlers → returns a response
- **Tech stack:** Java 25 · Spring Boot 4.0.6 · Spring AI 2.0.0-M5 · SQLite · jte templates · Gradle
- **Entry point:** `NcbotApplication.java` in the root package
- **Config:** `application.yaml` (or env vars prefixed `NCBOT_`)
- **Admin UI:** `http://localhost:8080/admin`

## Project Structure (by package)

```
org.huebert.ncbot/
├── NcbotApplication.java          # @SpringBootApplication, main entry
├── config/                        # Configuration properties
├── controller/                    # HTTP endpoints
│   ├── ChatController             # POST /v1/chat — the main public API
│   └── AdminController            # Admin UI routes (/admin/*)
├── service/                       # Business logic
│   ├── ChatService                # Core message processing, orchestrates handler chain
│   ├── MemoryService              # Scheduled AI memory synthesis
│   ├── TemplateService            # jte template rendering
│   └── WeatherService             # Open-Meteo weather API client
├── handler/                       # Request handlers (ordered chain)
│   ├── ChatHandler                # Interface — handlers implement this and are ordered
│   ├── AiChatHandler              # Calls OpenAI-compatible model with memory + context
│   ├── CommandChatHandler         # Shortcut commands (help, ping, etc.)
│   ├── WelcomeChatHandler         # Greets new participants
│   └── command/                   # Individual command handlers (one per command)
├── tool/                          # AI tools exposed to the model
├── entity/                        # JPA entities
├── repository/                    # JPA repositories
├── dto/                           # Request/response DTOs
└── util/                          # Utility classes
```

Resources live under `src/main/resources/` (application config, static assets) and `src/main/jte/` (jte templates for admin UI and AI prompts).

## How Messages Flow

```
POST /v1/chat (Controller)
    └── ChatService.processMessage()
            1. Skip if outgoing message (don't reply to self)
            2. Resolve or create ChatChannel entity
            3. Run handler chain (ordered, first match wins):
                 a. WelcomeChatHandler  — greets new participants
                 b. CommandChatHandler  — handles shortcuts like !help, !ping
                 c. AiChatHandler       — calls OpenAI-compatible model with memory + context
            4. Save message + response to SQLite
            5. Truncate to maxReplyBytes (128)
            6. Apply minimumResponseMs delay
```

The handler chain is ordered by `getOrder()` on the interface — lower values run first. `AiChatHandler` is the last resort.

## Admin UI

The admin UI is served at `/admin` and built with jte templates + Spring MVC. It uses HTMX for partial page updates.

**Routes:**
- `/admin` — Dashboard: message stats, uptime, channel stats
- `/admin/messages` — Message log viewer (paginated)
- `/admin/config` — View current configuration

**Tech:** jte server-side templates (`src/main/jte/admin/`), HTMX for AJAX, custom CSS (`static/css/admin.css`).

## Key Concepts

### Channel Configuration
Channels are defined in config under `ncbot.channels`. Each has flags:
- `ai` — enable AI responses
- `welcome` — greet new participants
- `command` — enable command shortcuts

DMs are controlled separately via `ncbot.allowed-dms` (set of hex keys).

### Memory System
- `MemoryService` runs on a schedule (`NCBOT_MEMORY_UPDATE_PERIOD`, default 30m)
- Reads message partitions (`NCBOT_MEMORY_PARTITION_SIZE`, default 100)
- Sends them to AI for key-value memory synthesis
- Memories are included in every AI chat prompt as `CHAT_MEMORY`
- Memory keys use dot-separated namespaces: `user.*`, `channel.*`, `bot.*`

### AI Prompt Assembly
Templates in `jte/prompts/` assemble context blocks:
- `chat.jte` — main prompt (memories + messages + request)
- `condense.jte` — for compressing oversized responses
- `memory.jte` — for memory synthesis
- `welcome.jte` — for welcome messages

### Tools Available to AI
- `getCurrentWeather(lat, lon)` — Open-Meteo API (temperature, wind, humidity, conditions)
- `getKnownChannels()` — list all channels the bot has seen
- `searchUsers(name)` — search users by substring

## Building & Running

```bash
# Local development (requires JDK 25)
./gradlew bootRun

# Build (production)
./gradlew build

# Run tests
./gradlew test

# Docker
docker compose build && docker compose up -d
```

## Important Details

- **Records & Lombok** — DTOs and config use Java records; entities use Lombok annotations (`@Slf4j`, builders)
- **SQLite** — JPA `ddl-auto: update`, dialect is `SQLiteDialect`
- **Database** — inside container: `/data/ncbot.db` (mounted from `./data/`)
- **jte templates** — compiled at build time from `src/main/jte/`
- **Logging** — root is `WARN`, `org.huebert` is `DEBUG`
- **Response delay** — `NCBOT_MINIMUM_RESPONSE_MS` pads slow responses (default 3000ms)
- **Condensing** — if AI response exceeds 128 bytes, a second AI call compresses it (enabled by default)
- **bot.py** — uses only Python stdlib, 9-second timeout, graceful failure (returns `None`)

## Extending the Bot

### Adding a New Command
1. Create a new class in the `handler/command/` package implementing `ChatHandler`
2. Set an appropriate `getOrder()` value (higher than `AiChatHandler`, lower than `WelcomeChatHandler`)
3. Add a jte prompt template in `jte/prompts/command/`
4. Register the command in the `CommandChatHandler`'s command map

### Adding a New AI Tool
1. Create a `@Component` class in the `tool/` package
2. Use Spring AI's `@Tool` annotation to expose methods
3. Add to `AiChatHandler`'s `ChatClient.defaultTools()`

### Adding a New Handler
1. Implement the `ChatHandler` interface in the `handler/` package
2. Set `getOrder()` to control execution position
3. Register as a Spring `@Component` — it will be auto-injected into `ChatService`

### Adding Admin UI Pages
1. Add a route method to `AdminController`
2. Create a jte template in `jte/admin/`
3. Use `_layout.jte` as the base layout (partial includes for headers/footers)
4. HTMX can be used for AJAX-driven partial updates

## Troubleshooting

- **No responses?** Check channel config has `ai: true` and channel name matches exactly
- **DMs not working?** Add sender key to `ncbot.allowed-dms`
- **Responses too long?** Enable condensing or reduce `NCBOT_MAX_REPLY_BYTES`
- **Slow?** Reduce `NCBOT_MINIMUM_RESPONSE_MS` or use a faster model
- **Check logs:** `docker compose logs ncbot` or run locally with `./gradlew bootRun`
