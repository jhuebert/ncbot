# Admin Interface Redesign Plan

## Overview

Complete rewrite of the `/admin` interface from the current multi-page table-based layout to a modern, single-page chat-style application with channel navigation, infinite-scroll message history, and per-channel/global memory management.

---

## Tech Decisions

| Concern | Choice | Rationale |
|---|---|---|
| Dynamic UI | **HTMX** | Server-rendered HTML fragments, no JS framework, clean progressive enhancement |
| CSS | **None (hand-rolled)** | Monochromatic theme is easy to style from scratch; avoids bloat |
| Favicon / Logo | **🤖 robot emoji** | Zero dependencies, matches the whimsical tone |
| Template engine | **jte (existing)** | Server-rendered partials for HTMX targets |
| JS | **Minimal** | Only for tab switching and scroll-to-bottom on initial load |
| Content negotiation | **HTML fragments** | Endpoints return jte-rendered HTML for HTMX swaps (except DELETE → 204) |

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│  /admin  (GET) → admin/index.jte  →  HTMX page           │
│                                                          │
│  Single HTML page with HTMX attributes                   │
│  - Renders left pane (channel list)                      │
│  - Renders main area (chat view / memory / participants) │
│  - HTMX swaps in/out server-rendered HTML fragments      │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│  AdminController endpoints (JSON + HTML fragments)       │
│                                                          │
│  GET  /admin/channels       → HTML: channel list items   │
│  GET  /admin/messages/{id}  → HTML: message list         │
│  GET  /admin/messages/{id}/older → HTML: older messages │
│  GET  /admin/memory/{id}    → HTML: memory list          │
│  GET  /admin/memory/global  → HTML: global memory list   │
│  POST /admin/memory         → HTML: new memory item      │
│  PUT  /admin/memory/{id}    → HTML: updated memory item  │
│  DEL  /admin/memory/{id}    → 204 No Content             │
│  POST /admin/memory/promote → HTML: promoted item        │
│  GET  /admin/participants/{id} → HTML: participants list │
└──────────────────────────────────────────────────────────┘
```

---

## UI Layout

### Desktop (≥ 768px)

```
┌─────────────────────────────────────────────────────────────────┐
│  🤖 ncbot admin                                                 │
├──────────────┬──────────────────────────────────────────────────┤
│              │  Tab bar: [Messages] [Memory] [Participants]     │
│  CHANNELS    │                                                  │
│              │  ┌──────────────────────────────────────────┐    │
│  ─ Public    │  │  @user1  Hello!                          │    │
│  ─ General   │  │  @ncbot    Hi there!                     │    │
│  ─ ...       │  │                                          │    │
│              │  │  (scroll up to load older)               │    │
│  DMs         │  └──────────────────────────────────────────┘    │
│  ─ @user1    │                                                  │
│  ─ @user2    │                                                  │
│  ─ ...       │                                                  │
│              │                                                  │
│  Global Mem  │                                                  │
│  Participants│                                                  │
└──────────────┴──────────────────────────────────────────────────┘
```

### Mobile (< 768px)

- Left pane becomes a **slide-out drawer** (hamburger menu icon)
- Tabs become a **horizontal scrollable bar** or **bottom tab bar**
- Memory view uses a **full-screen overlay** when selected
- Channel list drawer slides in from the left with a backdrop overlay

---

## Left Pane — Channel List

### Sections

1. **Public Channels** (is_dm = false, channel_name ≠ 'Public')
2. **DMs** (is_dm = true)
3. **Global Memory** (special item, not tied to any channel)
4. **Participants** (special item)

### Behavior

- Clicking a channel loads its messages (Messages tab, default) via HTMX `hx-get`
- Clicking "Global Memory" loads global memory (Memory tab, default)
- Clicking "Participants" shows participants list
- Each channel shows its `channelName`
- Channels sorted alphabetically within each section
- The currently active channel is highlighted

### Data Source

Server renders channel list items as HTML fragments. Initial load populates the left pane on page load.

```
GET /admin/channels
Response: HTML fragment of <div class="channel-item">...</div> elements
```

---

## Right Pane — Tabs

### Tab 1: Messages (default)

- Chat-like interface, messages stacked vertically, latest at bottom
- Each message shows:
  - **Sender name** (bold or distinct)
  - **Content**
  - **Timestamp** (relative, e.g. "2m ago", or compact ISO)
- Bot messages visually distinct (e.g. slightly different background or a "🤖" prefix)
- **Infinite scroll**: when the user scrolls to the top, fetch older messages (paginated by `createdAt` cursor or page number) and prepend them
- Initial load shows the 25 most recent messages
- Smooth scroll-to-bottom on initial load and on new messages

#### HTMX Integration

- **Initial load**: Server renders the first 25 messages on page load
- **Infinite scroll**: `hx-get` with `hx-trigger="scroll from:body scrolled; setInterval 2s"` (debounced)
  - When scroll reaches top, fetch older messages and `hx-swap="beforeend"` into the message container
  - Pass `before` timestamp as query param
- **Scroll-to-bottom**: Minimal JS on initial load and after HTMX swap

```
GET /admin/messages/{channelId}?before={timestamp}
Response: HTML fragment of <div class="message">...</div> elements
```

- `before` is an ISO timestamp of the oldest message currently visible
- Returns messages with `createdAt < before`, newest first, up to 25
- Returns empty fragment when no more messages exist

---

### Tab 2: Memory

- Displays key-value pairs for the selected channel
- **Global Memory** is the same view but with `chatChannelId IS NULL`
- Each memory entry shows:
  - **Key** (mono font, clickable to edit)
  - **Value** (editable)
  - **Delete button** (🗑️ or "×")
- **Add new** button at the top
- Inline editing: clicking a key/value makes it an input field
- Save/Cancel buttons appear during edit mode
- Promote button (📤) next to each entry to promote it to global memory

#### HTMX Integration

- **Initial load**: Server renders the memory list on page load
- **Add new**: `hx-post` to `/admin/memory` with form data, `hx-swap="afterbegin"` into the list
- **Edit inline**: Click to convert key/value to `<input>`, save via `hx-put`
- **Delete**: `hx-delete` with `hx-swap="outerHTML"` on a wrapper div (swap to empty div)
- **Promote**: `hx-post` to `/admin/memory/promote`, `hx-swap="afterbegin"` into the global list

```
GET  /admin/memory/{channelId}
Response: HTML fragment of memory items

POST /admin/memory
Body: channelId=1&key=test.key&value=test value
       (channelId omitted or null for global)
Response: HTML fragment of the new memory item

PUT  /admin/memory/{id}
Body: key=test.key&value=new value
Response: HTML fragment of the updated item

DELETE /admin/memory/{id}
Response: 204 No Content

POST /admin/memory/promote
Body: memoryId=1
Response: HTML fragment of the promoted (global) memory item
```

---

### Tab 3: Participants

- Simple list of participants for the selected channel
- Shows:
  - **Name**
  - **Last seen** (relative time)
- Populated from `ChatParticipant` table
- For channels, participants are inferred from unique `senderName` values in `ChatMessage`

#### HTMX Integration

- **Load on selection**: `hx-get` when participants tab is activated
- Server renders participant list items

```
GET /admin/participants/{channelId}
Response: HTML fragment of participant list items
```

> Note: `ChatParticipant` table currently tracks global participants by name. We may need to either query unique senders from messages per channel, or add a `channel_id` join. For simplicity, the first version will query distinct `senderName` from `ChatMessage` per channel.

---

## Global Memory

- Accessed via the "Global Memory" item in the left pane
- Functionally identical to channel memory (CRUD + promote)
- `chatChannelId` is `NULL` for all entries
- Promoting a channel memory entry creates a new entry with the same key/value but `chatChannelId = NULL`
- The original channel entry is **not** deleted (promote = copy to global)

---

## New Repository Methods

### ChatMessageRepository

```java
List<ChatMessage> findMessagesByChannelOrderByCreatedDesc(Long channelId, int limit);

List<ChatMessage> findMessagesByChannelBefore(Long channelId, Instant before, int limit);
```

### ChatChannelRepository

```java
// Already exists: findPublicChannels()
// Need: findDmChannels()
@Query("SELECT c FROM ChatChannel c WHERE c.isDm = true ORDER BY c.channelName ASC")
List<ChatChannel> findDmChannels();
```

### ChatMemory2Repository

```java
// Already exists: findMemory(Long chatChannelId)
// Need: findGlobalMemory()
@Query("SELECT m FROM ChatMemory m WHERE m.chatChannelId IS NULL ORDER BY m.key ASC")
List<ChatMemory> findGlobalMemory();
```

---

## New Controller Endpoints

All new endpoints live in `AdminController`. They return **HTML fragments** (jte templates) for HTMX to swap in, except DELETE which returns 204.

| Method | Path | Response |
|---|---|---|
| GET | `/admin/channels` | HTML fragment: channel list items |
| GET | `/admin/messages/{id}` | HTML fragment: first 25 messages |
| GET | `/admin/messages/{id}/older` | HTML fragment: older messages |
| GET | `/admin/memory/{id}` | HTML fragment: memory list |
| GET | `/admin/memory/global` | HTML fragment: global memory list |
| POST | `/admin/memory` | HTML fragment: new memory item |
| PUT | `/admin/memory/{id}` | HTML fragment: updated memory item |
| DELETE | `/admin/memory/{id}` | 204 No Content |
| POST | `/admin/memory/promote` | HTML fragment: promoted global item |
| GET | `/admin/participants/{id}` | HTML fragment: participant list |

---

## Files to Create / Modify

### Create

| File | Purpose |
|---|---|
| `src/main/jte/admin/index.jte` | Main HTMX page — navbar, left pane, right pane with tabs |
| `src/main/jte/admin/_channel_item.jte` | HTMX partial: single channel list item |
| `src/main/jte/admin/_message_item.jte` | HTMX partial: single message bubble |
| `src/main/jte/admin/_memory_item.jte` | HTMX partial: single memory key-value row |
| `src/main/jte/admin/_participant_item.jte` | HTMX partial: single participant row |
| `src/main/java/org/huebert/ncbot/controller/AdminController.java` | Add all new endpoints |

### Modify

| File | Purpose |
|---|---|
| `src/main/jte/admin/_layout.jte` | Simplify — just navbar + favicon + viewport meta |
| `src/main/resources/static/css/admin.css` | Complete rewrite — monochromatic, responsive, chat-style |
| `src/main/java/org/huebert/ncbot/repository/ChatMessageRepository.java` | Add `findMessagesByChannelBefore` |
| `src/main/java/org/huebert/ncbot/repository/ChatChannelRepository.java` | Add `findDmChannels()` |
| `src/main/java/org/huebert/ncbot/repository/ChatMemory2Repository.java` | Add `findGlobalMemory()` |
| `src/main/java/org/huebert/ncbot/repository/ChatParticipantRepository.java` | Add `findParticipantsByChannel(Long channelId)` |
| `build.gradle` | Add HTMX dependency |

### Delete (no longer needed)

| File | Can be removed |
|---|---|
| `src/main/jte/admin/messages.jte` | Replaced by HTMX page |
| `src/main/jte/admin/config.jte` | Not in scope |
| `src/main/jte/admin/index.jte` (old) | Replaced by new HTMX version |

---

## Design System (Monochromatic)

```css
:root {
  --bg:          #0a0a0a;      /* page background         */
  --surface:     #141414;      /* cards, panels           */
  --surface-2:   #1e1e1e;      /* hover, active           */
  --surface-3:   #2a2a2a;      /* borders, dividers       */
  --text:        #e0e0e0;      /* primary text            */
  --text-2:      #888888;      /* secondary / muted       */
  --text-3:      #555555;      /* tertiary / disabled     */
  --accent:      #666666;      /* active / focus          */
  --accent-2:    #999999;      /* hover on accent         */
  --bot-bg:      #1a1a1a;      /* bot message bubble      */
  --user-bg:     #222222;      /* user message bubble     */
  --danger:      #cc4444;      /* delete / error          */
  --success:     #44aa66;      /* save / confirm          */
}
```

- All colors are grayscale with minimal tint
- No gradients, no shadows (or very subtle)
- Rounded corners: 6px
- Font: system font stack (same as current)
- Message bubbles: left-aligned, max-width ~70%, with sender name above

---

## Mobile Responsive Strategy

1. **Hamburger menu** (☰) in navbar toggles the left channel list as a slide-in drawer
2. **Backdrop overlay** dims the main content when drawer is open
3. **Tabs** move to a scrollable horizontal strip below the navbar
4. **Memory form**: full-width inputs, stacked vertically
5. **Participants**: simple list, no extra columns
6. Touch targets ≥ 44px
7. Use CSS media queries at 768px breakpoint

---

## Default Landing Page

- `/admin` loads the Public channel's messages by default
- The "Public" channel is selected in the left pane
- The Messages tab is active

---

## Implementation Order

1. **Dependencies** — add HTMX to `build.gradle`
2. **Repository methods** — add new queries for DM channels, global memory, per-channel participants, older messages
3. **Layout** — simplify `_layout.jte` with favicon and minimal structure
4. **CSS** — complete rewrite: monochromatic, responsive, chat-style
5. **Channel list** — server-render initial channel list, HTMX `hx-get` for selection
6. **Messages tab** — server-render initial messages, HTMX infinite scroll
7. **Memory tab** — server-render memory list, HTMX CRUD + promote
8. **Participants tab** — HTMX load on selection
9. **Global memory** — wire up Global Memory left-pane item
10. **Mobile responsive** — drawer, tab bar, touch targets

---

## Notes

- Old endpoints (`/admin/messages`, `/admin/config`) and old `index.jte` are **removed** — no backward compatibility needed
- The existing `memory-update-period` scheduled job continues to work independently
- No changes to the AI memory synthesis pipeline — this is purely a UI/admin tool change
- Memory CRUD is manual only; the AI-driven auto-update is separate
- HTMX partials keep the code simple: each endpoint returns a small jte fragment, no JS templates needed
- Minimal JS is only used for: tab switching, scroll-to-bottom after HTMX swaps, and the mobile drawer toggle
