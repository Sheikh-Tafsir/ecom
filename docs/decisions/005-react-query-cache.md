# ADR 005 — React Query Cache Structure for Chat

**Status:** Accepted  
**Date:** 2024

## Decision

The selected chat is stored in React Query as an **infinite query** where `pages[0]` always holds the most recent messages and grows as new messages arrive, while `pages[1+]` hold older pages loaded by scrolling up.

## Cache Keys

| Key                        | Shape              | Description                                          |
|----------------------------|--------------------|------------------------------------------------------|
| `['chats']`                | `Chat[]`           | Chat list with unread counts and last message        |
| `['selected_chat', id]`    | `{ pages: Page[] }`| Infinite query — paginated message history           |

## Page Structure

```
pages[0] = { messages: [oldest...newest], pagination, ...chatDetails }   ← grows with new messages
pages[1] = { messages: [...], pagination }                                ← older (from scroll up)
pages[2] = { messages: [...], pagination }                                ← even older
```

- Messages **within** each page are in chronological order (oldest → newest).
- Pages are ordered **newest → oldest** — `pages[0]` is always the most recent window.
- New messages (from WebSocket or sync) are appended to `pages[0].messages`.

## Why This Structure

- **UX requirement:** Chat UI renders newest messages at the bottom, loads older messages by scrolling up (infinite scroll upward).
- **Efficient live updates:** Appending to `pages[0]` avoids re-rendering the entire message list — only the latest page is mutated.
- **Backward compatible with TanStack Query v5 `useInfiniteQuery`:** The page-based structure maps directly to the library's internal shape.

## Hook Responsibilities

| Hook              | Purpose                                                                 |
|-------------------|-------------------------------------------------------------------------|
| `useChatData.js`  | Data fetching — chat list (`useQuery`), selected chat (`useInfiniteQuery`) |
| `useChatSync.js`  | Real-time sync — WS listeners, reconnect recovery, dedup, mark-as-seen |
| `useChatActions.js`| User actions — send message, group management (WS emit)               |

## Message Deduplication

All insertions into `pages[0].messages` check for existing `message.id` before appending. This prevents duplicates when:
- A `receive-message` WS event and a sync HTTP response deliver the same message.
- Multiple rapid reconnections fire in quick succession.

The dedup key is `message.id` (PostgreSQL BIGSERIAL) — globally unique, monotonically increasing.

## Trade-offs Accepted

| ✅ Benefit                                  | ⚠️ Cost                                                       |
|---------------------------------------------|---------------------------------------------------------------|
| Efficient append — only `pages[0]` mutated  | Page structure is non-obvious; requires careful cache writes  |
| Native fit with `useInfiniteQuery`          | Deduplication logic must be applied consistently on every insert |
| Supports both scroll-up load and reconnect sync | Full refetch required when > 15 messages missed on reconnect |
