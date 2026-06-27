# ncbot — Agent Guide

**AI chat bot for [Meshcore](https://meshcore.io/)** — receives chat messages via HTTP, processes them through a chain of handlers, runs them through an OpenAI-compatible model, and returns short responses (≤ 128 UTF-8 bytes).

## TL;DR

| Item | Value |
|---|---|
| **Entry point** | `NcbotApplication.java` (root package) |
| **Config** | `src/main/resources/config/application.yml` or env vars prefixed `NCBOT_` |
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
├── config/                        # @ConfigurationProperties
│   ├── AiMode                     # DISABLED, EACH, TAGGED
│   ├── ChannelCapabilities        # Resolved channel capabilities
│   └── NcbotProperties
├── controller/                    # HTTP endpoints
│   ├── ChatController             # POST /v1/chat
│   ├── ChannelsController         # /v1/channels CRUD
│   ├── MessagesController         # /v1/channels/{id}/messages
│   ├── MemoryController           # /v1/memory CRUD + /v1/channels/{id}/memory
│   └── ParticpantsController      # /v1/participants (note: typo in class name)
├── service/
│   ├── ChatService                # Orchestrates handler chain
│   ├── MemoryService              # Scheduled memory synthesis
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
├── tool/                          # AI tools (@Component + @Tool)
│   └── WeatherTool.java           # getCurrentWeather
├── entity/                        # JPA entities (Lombok)
│   ├── ChatChannel, ChatMemory, ChatMessage, ChatParticipant
├── repository/                    # JPA repositories
│   ├── ChatChannelRepository
│   ├── ChatMemory2Repository
│   ├── ChatMessageRepository
│   └── ChatParticipantRepository
├── controller/dto/                # API response DTOs (records)
│   ├── ChannelDto, MessageDto, MessagesResponse
│   ├── MemoryDto, MemoryCreateRequest, MemoryUpdateRequest
│   ├── ParticipantDto, PageResponse
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
| `BlockingChatHandler` | 200 | Block user/path by regex |
| `WelcomeChatHandler` | 100 | Greet new participants |
| `PathUpgradeChatHandler` | 75 | Notify users to upgrade path hash |
| `PathFilterChatHandler` | 60 | Conditionally block 1-byte paths (`NCBOT_ALLOW_ONE_BYTE_PATHS`) |
| `CommandChatHandler` | 50 | Match shortcut commands |
| `AiChatHandler` | -100 | Last resort — fallback to AI |

**Short-circuit:** Handlers return `Optional.of("")` (empty string) to signal a block. `ChatService.generateResponse()` detects this and returns `EMPTY_RESPONSE` without saving.

---

## Channel Configuration

Channels are defined via regex patterns in `application.yml`. Each pattern is matched against the channel name (e.g. `#ncbot`).

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

**Other flags** (`welcome`, `command`, `path-upgrade`) are independent — presence = `true`, absence = `false`.

**DMs:** Controlled by `ncbot.allowed-dms` (comma-separated hex keys). DMs always have `ai: EACH`, `welcome: true`, `command: true` if the sender key is in the allowed list.

---

## Blocking & Filtering

### User/Path Blocking

Regex patterns in `application.yml`:

```yaml
ncbot:
  block-user: ".*(bot|spam|scam).*"
  allow-user: "admin.*"
  block-path: ".*malicious.*"
  allow-path: "internal.*"
```

**Precedence:** allow always beats block. If a user/path matches an allow pattern, they are allowed regardless.

### Path Filtering

1-byte paths are **allowed by default** (`NCBOT_ALLOW_ONE_BYTE_PATHS=true`). Set to `false` to block 1-byte paths from reaching command and AI handlers. Welcome and path-upgrade notifications still work for blocked paths.

---

## Memory System

- `MemoryService` runs on a schedule (`NCBOT_MEMORY_UPDATE_PERIOD`, default 30m)
- Reads message partitions (`NCBOT_MEMORY_PARTITION_SIZE`, default 100)
- Sends them to AI for key-value memory synthesis
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
| `/v1/channels` | GET | All channels (`?dm=true\|false`) |
| `/v1/channels/{channelId}` | DELETE | Delete channel + cascade |
| `/v1/channels/{channelId}/messages` | GET | Messages (`?page`, `?size`, `?before=ISO-instant`, `?after=ISO-instant`, `?sortDirection=ASC\|DESC`) |
| `/v1/channels/{channelId}/memory` | GET/POST | Channel memories |
| `/v1/channels/{channelId}/memory/{id}` | PUT/DELETE | Update/delete channel memory |
| `/v1/channels/{channelId}/memory/{id}/promote` | POST | Promote to global |
| `/v1/channels/{channelId}/participants` | GET | Channel participants |
| `/v1/memory` | GET/POST | Global memories |
| `/v1/memory/{id}` | PUT/DELETE | Update/delete global memory |
| `/v1/participants` | GET | All participants with last seen |

**Validation rules:**
- Channel memory endpoints reject where memory's `chatChannelId` ≠ path parameter
- Global memory endpoints reject where memory has non-null `chatChannelId`
- Promote validates source belongs to specified channel, copies to global, deletes source

**Request DTOs** (all in `controller.dto` package):

| DTO | Fields |
|---|---|
| `MemoryCreateRequest` | `key`, `value` |
| `MemoryUpdateRequest` | `key`, `value` |

**Response DTOs** (all in `controller.dto` package):

| DTO | Fields |
|---|---|
| `ChannelDto` | `id`, `channelKey`, `channelName`, `isDm` |
| `MessageDto` | `id`, `senderName`, `content`, `createdAt` |
| `MessagesResponse` | `channelId`, `channelName`, `messages[]`, `totalPages`, `currentPage`, `totalElements` |
| `MemoryDto` | `id`, `channelId`, `key`, `value` |
| `ParticipantDto` | `name`, `lastSeen` |
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

### Testing

- Spring Boot Test with JUnit Platform
- Run with `./gradlew test`
- Main test class: `NcbotApplicationTests.java`

### Documentation

- Update all three docs after code changes: `AGENTS.md`, `README.md`, `openapi.yml`
- Environment variables use `NCBOT_` prefix; application.yml uses snake_case (e.g., `NCBOT_AI_ENABLED` ↔ `ncbot.ai-enabled`)

---

## Extending the Bot

### Adding a New Command

1. Create a class in `handler/command/` implementing `CommandHandler`
2. Define the command pattern as a `Pattern` constant (regex, case-insensitive by convention)
3. Return a `Map<String, Object>` with a `template` key pointing to a jte template
4. Add a jte template in `src/main/jte/command/`

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
| DMs not working | Sender key not in `allowed-dms` | Add hex key to `ncbot.allowed-dms` |
| Responses too long | Condensing disabled or limit too high | Enable condensing or reduce `NCBOT_MAX_REPLY_BYTES` |
| Slow responses | High `NCBOT_MINIMUM_RESPONSE_MS` or slow model | Reduce delay or use faster model |
| Template errors | jte compile failure | Check `src/main/jte/` syntax; run `./gradlew build` |
| User blocked unexpectedly | Regex in `block-user` matches | Check patterns; use `allow-user` to whitelist |
| 1-byte path messages not responding | `NCBOT_ALLOW_ONE_BYTE_PATHS=false` or path is 1-byte | Check setting; set to `true` to allow, or handler order is correct |

**Check logs:** `docker compose logs ncbot` or `./gradlew bootRun`
