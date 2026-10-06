# ADR 004 — WebSocket Chat Without a Message Queue

**Status:** Accepted  
**Date:** 2024

## Decision

Real-time chat message delivery uses **direct Socket.IO room broadcast** to connected clients. There is no RabbitMQ or dedicated message broker between the Chat Server and the React client.

## Why

- **Simplicity** — adding a message queue between the chat server and the client adds latency, a new infrastructure dependency, and consumer complexity for a feature that doesn't require guaranteed delivery to offline clients.
- **PostgreSQL as source of truth** — every message is persisted to PostgreSQL before being broadcast. Clients that missed messages (due to disconnect) recover via the existing HTTP forward-pagination endpoint (`GET /chats/:id?afterId=`), not via queue replay.
- **Redis is already in the stack** — Socket.IO's built-in Redis adapter handles multi-node pub/sub if the chat server scales horizontally, without a separate broker.

## Delivery Guarantee

The system provides **at-least-once delivery** with deduplication:

```
Client sends message
    → Chat Server persists to PostgreSQL (source of truth)
    → Chat Server broadcasts via Socket.IO to chat room
    → If client was offline: recovered via GET /chats/:id?afterId=<lastId>
    → Deduplication by message.id prevents duplicates on reconnect
```

## Rate Limiting

- Hard limit: **5 messages per second per socket connection**, enforced in the Chat Server.
- Excess messages are dropped silently at the socket layer.

## Reconnect Recovery (No Dedicated Sync Endpoint)

On Socket.IO reconnect:
1. `invalidateQueries(['chats'])` — refetches chat list (unread counts, last message ordering).
2. If a chat is open: `GET /chats/:id?afterId=<lastCachedMsgId>` — fetches only missed messages.
3. If too many missed (> 15, `hasMore = true`): `invalidateQueries(['selected_chat', id])` for full refetch.

## Trade-offs Accepted

| ✅ Benefit                               | ⚠️ Cost                                                        |
|------------------------------------------|----------------------------------------------------------------|
| No additional broker infrastructure      | No guaranteed delivery to offline clients via push             |
| Low latency (direct socket broadcast)    | Recovery depends on client initiating a reconnect flow         |
| Simple operational model                 | If Socket.IO broadcast fails, message is still in DB (recoverable but not instant) |
