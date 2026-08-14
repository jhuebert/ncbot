# ncbot — Agent Guide

**AI chat bot for [Meshcore](https://meshcore.io/)** — receives chat messages via HTTP, processes them through a chain of handlers, runs them through an OpenAI-compatible model, and returns short responses (≤ 128 UTF-8 bytes).

## TL;DR

| Item | Value |
|---|---|
| **Entry point** | `NcbotApplication.java` (root package) |
| **Config** | DB `config_item` table, managed via Settings UI (`/v1/config`); startup-only values in `application.yml` / `NCBOT_*` env vars |
| **Chat API** | `POST /v1/chat` — public, no auth |
| **Admin API** | `GET/POST/PUT/DELETE http://localhost:8080/v1/*` — no auth (see openapi.yml) |
| **DB** | SQLite at `./data/ncbot.db` (mounted as `/data` in Docker) |
| **Tests** | `./gradlew test` → `NcbotApplicationTests.java` |

Full README (setup, deployment, RemoteTerm integration): see `README.md`.

---

## Project Structure

```
org.huebert.ncbot/
├── NcbotApplication.java          # @SpringBootApplication, main entry
├── config/                        # Configuration types + registry
│   ├── AiMode                     # DISABLED, EACH, TAGGED
│   ├── ChannelCapabilities        # Resolved channel capabilities
│   ├── ConfigItemDefinition       # Registry of DB-backed config items (key/type/default)
│   ├── ConfigType                 # STRING, TEXT, BOOLEAN, INT, LONG, LIST
│   └── NcbotProperties            # Startup-only config (ai/weather/memory timeouts)
├── controller/                    # HTTP endpoints (thin — delegates to services)
│   ├── ApiExceptionHandler         # IllegalArgumentException → HTTP 400
│   ├── ChatController             # POST /v1/chat
│   ├── ChannelsController         # /v1/channels CRUD
│   ├── ConfigController           # /v1/config get/update/reset
│   ├── MessagesController         # /v1/channels/{id}/messages
│   ├── MemoryController           # /v1/memory CRUD + /v1/channels/{id}/memory
│   └── ParticipantsController     # /v1/participants
├── service/
│   ├── ChannelService             # Channel CRUD (delete with cascade)
│   ├── ChatService                # Orchestrates handler chain
│   ├── ConfigService              # DB-backed config: seeding, typed reads, validation, channel caps
│   ├── AiService                  # Forced AI reply (shared by AiChatHandler + AI-invoking commands)
│   ├── MemoryService              # Scheduled memory synthesis + memory CRUD
│   ├── ParticipantService         # Participant queries
│   ├── TemplateService            # jte rendering
│   └── WeatherService             # Open-Meteo client
├── handler/                       # Ordered handler chain
│   ├── ChatHandler                # Interface with getOrder()
│   ├── AiChatHandler              # ORDER -100 — Spring AI fallback
│   ├── BlockingChatHandler        # ORDER 200 — user/path blocking
│   ├── CommandChatHandler         # ORDER 50 — shortcut commands
│   ├── PathFilterChatHandler      # ORDER 60 — conditionally blocks 1-byte paths
│   ├── PathUpgradeChatHandler     # ORDER 75 — path upgrade notice
│   ├── WelcomeChatHandler         # ORDER 100 — new participants
│   └── command/                   # Individual command handlers
│       ├── ChannelsChatHandler, HelpChatHandler, PathChatHandler
│       ├── DiceChatHandler, PingChatHandler, RandomChatHandler, TestChatHandler, UsersChatHandler
│       ├── WxChatHandler          # wx / weather — AI-invoking weather command
├── tool/                          # AI tools (@Component + @Tool)
│   ├── WeatherTool.java           # getWeather (current + 7-day forecast)
│   ├── HistoryTool.java           # getHistory (on-demand channel history/search)
│   ├── ParticipantTool.java       # getChannelParticipants (who's active in a channel)
│   ├── ByteLengthTool.java        # checkBytes (answer byte-length)
│   └── MemoryTool.java            # insert/update/deleteMemory (persist durable facts)
├── entity/                        # JPA entities (Lombok)
│   ├── ChatChannel, ChatMemory, ChatMessage, ChatParticipant, ConfigItem
├── repository/                    # JPA repositories
│   ├── ChatChannelRepository
│   ├── ChatMemory2Repository
│   ├── ChatMessageRepository
│   ├── ChatParticipantRepository
│   └── ConfigItemRepository
├── controller/dto/                # API response DTOs (records)
│   ├── ChannelDto, MessageDto, MessagesResponse
│   ├── ConfigItemDto, ConfigItemUpdateRequest
│   ├── MemoryDto, MemoryCreateRequest, MemoryUpdateRequest
│   ├── ParticipantDto, ParticipantUpdateRequest, PageResponse
├── dto/                           # Request/response DTOs (records)
│   ├── ChatRequest, ChatResponse
│   ├── WeatherApiResponse, WeatherCurrent, WeatherToolResponse
└── util/
    ├── Delay, Pair, Truncate
```

Resources: `src/main/resources/config/` (config) · `src/main/jte/` (jte templates)

---

## Handler Chain

Handlers implement `ChatHandler` with `getOrder()` — **larger values run first**. First matching handler short-circuits the chain.

| Handler | Order | Purpose |
|---|---|---|
| `BlockingChatHandler` | 200 | Block user/path/channel by regex or explicit participant flag |
| `WelcomeChatHandler` | 100 | Greet new participants |
| `PathUpgradeChatHandler` | 75 | Notify users to upgrade path hash |
| `PathFilterChatHandler` | 60 | Conditionally block 1-byte paths (`chat.allow-one-byte-paths`) |
| `CommandChatHandler` | 50 | Match shortcut commands |
| `AiChatHandler` | -100 | Last resort — fallback to AI |

**Short-circuit:** Handlers return `Optional.of("")` (empty string) to signal a block. `ChatService.generateResponse()` detects this and returns `EMPTY_RESPONSE` without saving.

---

## Channel Configuration

Channels are defined via regex patterns. These patterns (and all other runtime settings)
live in the database `config_item` table and are managed via the admin UI (**Settings**) or
`/v1/config` API. They are read through `ConfigService`.

```
channels.welcome        : "^#ncbot$"
channels.command        : "^#ncbot$"
channels.path-upgrade   : "^#ncbot$"
channels.ai-each        : "^#ncbot$"
channels.ai-tagged      : ".*"
```

**AI Mode Resolution:**
1. If `ai-enabled` is `false`, all channels default to `DISABLED`
2. If the channel matches `channels-ai-each`, mode is `EACH` (respond to every message)
3. If the channel matches `channels-ai-tagged`, mode is `TAGGED` (respond only when mentioned)
4. Default is `DISABLED` if the channel matches neither AI list

**Other flags** (`welcome`, `command`, `path-upgrade`) are independent — presence = `true`, absence = `false`.

**DMs:** Controlled by the DB `channels.allowed-dms` list (comma-separated hex keys). DMs always have `ai: EACH`, `welcome: true`, `command: true` if the sender key is in the allowed list.

---

## Database-Backed Configuration

Most runtime configuration is stored in the `config_item` table (entity `ConfigItem`, read via
`ConfigService`) rather than in `application.yml`. Every item is declared as a
`ConfigItemDefinition` enum constant — a one-line change that seeds its default on startup and
surfaces it in the admin UI automatically.

- **Keys** are dot-separated names loosely grouped by namespace (`bot.*`, `chat.*`, `memory.*`,
  `channels.*`, `blocking.*`).
- **Types** (`ConfigType`) drive validation and the UI editor: `STRING`, `TEXT`, `BOOLEAN`, `INT`,
  `LONG`, `LIST` (comma-separated).
- **Defaults** are seeded on a fresh installation; missing rows are re-seeded on startup.
- **Update** validates against the declared type; **reset** removes the override and reverts to default.
- `restartRequired` items (currently `chat.max-reply-tokens`) are stored in the DB and shown in the
  UI but only take effect after a restart.

### Remaining in application.yml

The following cannot realistically live in the DB because they are consumed only at startup / bean
construction, or are secrets / infrastructure:

| Setting | Why it stays in config |
|---|---|
| `spring.ai.openai.*` (api-key, base-url, model) | Secrets + wired into the AI client at startup |
| `ncbot.ai-timeout` | Baked into the OpenAI client at startup |
| `ncbot.memory-update-period` | Fixed-delay scheduler interval |
| `ncbot.weather-timeout` | Baked into the weather client at startup |
| `server.*`, `spring.datasource.*` | Infrastructure / boot |

These are the only fields left on `NcbotProperties` (`aiTimeout`, `memoryUpdatePeriod`,
`weatherTimeout`).

### Adding a Configuration Item

1. Add a `ConfigItemDefinition` enum constant with key, type, default, description and
   `restartRequired` flag.
2. (Optional) add a typed convenience accessor on `ConfigService` and use it wherever the value
   is consumed.
3. Restart — the row is seeded, and the item appears in the Settings UI automatically.

## Blocking & Filtering

### User/Path/Channel Blocking

Regex patterns stored in the DB (via Settings / `/v1/config`):

```
blocking.block-user  : ".*(bot|spam|scam).*"
blocking.allow-user  : "admin.*"
blocking.block-path  : ".*malicious.*"
blocking.allow-path  : "internal.*"
blocking.block-channel: ".*spam.*"
blocking.allow-channel: "^#trusted$"
```

**Precedence:** allow always beats block at every level (user > channel > path). If a user/path/channel matches an allow pattern, they are allowed regardless of block patterns.

**DMs:** Channel blocking is skipped for DMs — only user and path patterns apply.

### Per-Participant Blocking

In addition to the `blocking.block-user` regex, individual participants can be blocked directly via the admin API and frontend. Each participant row has a `blocked` boolean flag (`chat_participant.blocked`):

- `PUT /v1/participants/{participantId}` with body `{ "blocked": true }` blocks a participant; `{ "blocked": false }` unblocks
- The frontend (Participants page and channel detail → Participants tab) shows a Block/Unblock button per participant
- A blocked participant behaves like a `blocking.block-user` regex match — the bot ignores their messages, and `blocking.allow-user` regex still takes precedence
- The flag is stored in the database, so it survives restarts (unlike regex config)

### Path Filtering

1-byte paths are **allowed by default** (`chat.allow-one-byte-paths = true`). Set to `false` to block 1-byte paths from reaching command and AI handlers. Welcome and path-upgrade notifications still work for blocked paths.

---

## Memory System

- `MemoryService` runs on a schedule (`ncbot.memory-update-period`, default 30m, in application.yml)
- Reads message partitions (`memory.partition-size`, default 100, DB setting)
- Sends them to AI for key-value memory synthesis
- Each channel is synthesized independently (one channel's AI failure doesn't abort others)
- The memory-synthesis AI call retries up to 3 times per partition for transient provider failures
- Memories are included in every AI chat prompt as `CHAT_MEMORY`
- Memory keys use dot-separated namespaces: `user.*`, `channel.*`, `bot.*`

**Storage:** `chat_memory` table (entity `ChatMemory`, repository `ChatMemory2Repository`), scoped to each channel or global (`channelId: null`).

---

## AI Prompt Assembly

Templates in `jte/prompts/` assemble context blocks:

| Template | Purpose |
|---|---|
| `chat.jte` | Main prompt (memories + messages + request) |
| `condense.jte` | Compressing oversized responses |
| `memory.jte` | Memory synthesis |
| `welcome.jte` | Welcome messages |
| `command/*.jte` | Per-command prompt overrides |

**Constraints:** ≤ 128 bytes, `@[username]` mentions, no self-intro. Condensing is enabled by default — a second AI call compresses oversized responses.

---

## Admin API

Controllers live in `controller/` package. See `openapi.yml` for the full OpenAPI 3.1 spec (no auth).

**Pagination is 0-indexed** — use `?page=0&size=25` (default page 0, default size 25). All paginated endpoints return `PageResponse<T>`:

**Search** — the channels, memory, and participants list endpoints accept an optional `?query=` param for a case-insensitive substring search:

| Endpoint | Search matches |
|---|---|
| `GET /v1/channels` | channel name or key |
| `GET /v1/memory`, `GET /v1/channels/{id}/memory` | memory key or value |
| `GET /v1/participants`, `GET /v1/channels/{id}/participants` | participant name |

```json
{
  "content": [...],
  "totalPages": 5,
  "currentPage": 0,
  "totalElements": 123
}
```

| Path | Method | Description |
|---|---|---|
| `/v1/channels` | GET | All channels (`?dm=true\|false`, `?query=`), sorted by last message DESC |
| `/v1/channels/{channelId}` | DELETE | Delete channel + cascade |
| `/v1/channels/{channelId}/messages` | GET | Messages (`?page`, `?size`, `?before=ISO-instant`, `?after=ISO-instant`, `?sortDirection=ASC\|DESC`) |
| `/v1/channels/{channelId}/memory` | GET/POST | Channel memories (GET supports `?query=`) |
| `/v1/channels/{channelId}/memory/{id}` | PUT/DELETE | Update/delete channel memory |
| `/v1/channels/{channelId}/memory/{id}/promote` | POST | Promote to global |
| `/v1/channels/{channelId}/participants` | GET | Channel participants (`?query=`) |
| `/v1/memory` | GET/POST | Global memories (GET supports `?query=`) |
| `/v1/memory/{id}` | PUT/DELETE | Update/delete global memory |
| `/v1/participants` | GET | All participants with last seen (`?query=`) |
| `/v1/participants/{participantId}` | PUT | Block/unblock participant (body: `{blocked: true\|false}`) — `ParticipantDto` |
| `/v1/config` | GET | All DB-backed configuration items with type/description/default |
| `/v1/config/{key}` | PUT | Update a config item's value (validated by type) |
| `/v1/config/{key}` | DELETE | Reset a config item to its default |

**Validation rules:**
- Channel memory endpoints reject where memory's `chatChannelId` ≠ path parameter
- Global memory endpoints reject where memory has non-null `chatChannelId`
- Promote validates source belongs to specified channel, copies to global, deletes source

**Request DTOs** (all in `controller.dto` package):

| DTO | Fields |
|---|---|
| `MemoryCreateRequest` | `key`, `value` |
| `MemoryUpdateRequest` | `key`, `value` |
| `ParticipantUpdateRequest` | `blocked` |

**Response DTOs** (all in `controller.dto` package):

| DTO | Fields |
|---|---|
| `ChannelDto` | `id`, `channelKey`, `channelName`, `isDm`, `lastMessageAt` |
| `MessageDto` | `id`, `senderName`, `content`, `createdAt` |
| `MessagesResponse` | `channelId`, `channelName`, `messages[]`, `totalPages`, `currentPage`, `totalElements` |
| `MemoryDto` | `id`, `channelId`, `key`, `value` |
| `ParticipantDto` | `id`, `name`, `firstSeen`, `lastSeen`, `pathUpgradeNotifiedAt`, `blocked` |
| `ConfigItemDto` | `key`, `type`, `value`, `defaultValue`, `description`, `restartRequired`, `isDefault` |
| `PageResponse<T>` | `content[]`, `totalPages`, `currentPage`, `totalElements` |

---

## Development Guidelines

### Code Style & Conventions

- **Records** for DTOs and config properties; **Lombok** (`@Slf4j`, builders) for entities
- **Prefer records over classes** for data carriers
- **Handler interface** — all request handlers implement `ChatHandler` with `getOrder()`
- **Component registration** — all handlers, tools, and services are `@Component` beans (auto-discovered)
- **SQLite** — JPA `ddl-auto: update`, dialect is `SQLiteDialect` via `hibernate-community-dialects`
- **Braces required** on all conditionals, loops, etc.

### Transaction Boundaries

- **All controllers are thin** — business logic lives in services; controllers only parse input, call services, format output
- **`@Transactional` on every service method** that writes to the database — ensures atomicity of multi-step operations
- **`@Transactional(readOnly = true)` on read-only service methods** — enables Hibernate read-only optimizations
- **Multi-step operations** (e.g., delete channel with cascade, promote memory, create+save) are always wrapped in a single transaction
- **Services handle validation** — entity ownership checks, cross-entity consistency (e.g., memory belongs to channel) are in service methods, not controllers

### Testing

- Spring Boot Test with JUnit Platform
- Run with `./gradlew test`
- Main test class: `NcbotApplicationTests.java`

### Documentation

- Update all three docs after code changes: `AGENTS.md`, `README.md`, `openapi.yml`
- Remaining application.yml values take `NCBOT_` env vars (e.g. `NCBOT_AI_TIMEOUT` ↔ `ncbot.ai-timeout`). DB config items are set via the Settings UI / `/v1/config`.

---

## Extending the Bot

### Adding a New Command

1. Create a class in `handler/command/` implementing `CommandHandler`
2. Define the command pattern as a `Pattern` constant (regex, case-insensitive by convention)
3. Return a `Map<String, Object>` with a `template` key pointing to a jte template
4. Add a jte template in `src/main/jte/command/`

To make a command **reply via AI** (works even when the channel's AI mode is disabled or ncbot wasn't tagged), return `Map.of("ai", true)` instead of a `template`. `CommandChatHandler` then routes the reply through `AiService`. An optional `"aiMessage"` string overrides the user's raw text fed to the model — see `WxChatHandler` (`wx`/`weather`).

### Adding a New Configuration Item

1. Add a constant to `ConfigItemDefinition` (key, `ConfigType`, default, description, restart flag)
2. Optionally add a typed accessor on `ConfigService`
3. The value is seeded automatically on startup and appears in the Settings UI

### Adding a New AI Tool

1. Create a `@Component` class in the `tool/` package
2. Use Spring AI's `@Tool` annotation to expose methods
3. Add to `AiChatHandler`'s `ChatClient.defaultTools()`

### Adding a New Handler

1. Implement the `ChatHandler` interface in the `handler/` package
2. Set `getOrder()` to control execution position
3. Register as a Spring `@Component` — auto-injected into `ChatService`

---

## Security Notes

- **API keys** — passed via env vars or config; never commit to version control
- **SQLite database** — restrict file permissions on `./data/ncbot.db` in production
- **No authentication** on `/v1/*` — do not expose to untrusted networks
- **OpenAI endpoint** — verify TLS on `NCBOT_OPENAI_BASE_URL` in production

---

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---|---|---|
| No responses | Channel not in any AI list | Check `channels-ai-each` / `channels-ai-tagged` |
| DMs not working | Sender key not in allowed list | Add hex key to `channels.allowed-dms` (Settings) |
| Responses too long | Condensing disabled or limit too high | Enable condensing or reduce `chat.max-reply-bytes` (Settings) |
| Slow responses | High `chat.minimum-response-ms` or slow model | Reduce delay or use faster model |
| Template errors | jte compile failure | Check `src/main/jte/` syntax; run `./gradlew build` |
| User blocked unexpectedly | Regex in `blocking.block-user` matches, or participant `blocked` flag is set | Check patterns; use `blocking.allow-user` to whitelist, or unblock via admin API/frontend |
| 1-byte path messages not responding | `chat.allow-one-byte-paths=false` or path is 1-byte | Check setting (Settings); set to `true` to allow, or handler order is correct |

**Check logs:** `docker compose logs ncbot` or `./gradlew bootRun`
