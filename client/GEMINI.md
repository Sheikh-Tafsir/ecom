# Client Module (Frontend)

## Overview
A modern React application built with Vite and Tailwind CSS.

## Tech Stack
- **Framework:** React 18+ (JSX), Vite.
- **Styling:** Tailwind CSS, Shadcn/ui.
- **State:** Zustand (Global), React Hook Form (Local Forms).
- **Data:** TanStack Query (React Query) v5+.

## Chat Feature Architecture

### Hook Responsibilities (`features/chats/hooks/`)

| Hook | Purpose |
|------|---------|
| `useChatData.js` | Data fetching — chat list (`useQuery`), selected chat with infinite scroll (`useInfiniteQuery`) |
| `useChatSync.js` | Real-time sync — WS event listeners, reconnect sync, message dedup, mark-as-seen |
| `useChatActions.js` | User actions — send message, group management (emit WS events) |

### React Query Cache Keys

| Key | Shape | Description |
|-----|-------|-------------|
| `['chats']` | `Chat[]` | Chat list with metadata (unread count, last message) |
| `['selected_chat', id]` | `{ pages: Page[] }` | Infinite query — `pages[0]` = latest messages, `pages[1+]` = older pages |

### Cache Structure for Selected Chat
```
pages[0] = { messages: [oldest...newest], pagination, ...chatDetails }   ← latest page (grows with incoming messages)
pages[1] = { messages: [...], pagination }                                ← older messages (from scrolling up)
pages[2] = { messages: [...], pagination }                                ← even older
```
- Messages within each page are in **chronological order** (oldest first).
- Pages go from **newest to oldest** — `pages[0]` always contains the most recent messages.
- New incoming messages (WS or sync) are appended to `pages[0].messages`.

### Reconnect Synchronization (`useChatSync.js`)
When Socket.IO reconnects after a disconnect:

1. **Chat list** — `invalidateQueries(['chats'])` triggers a server refetch (fresh unread counts, last messages, ordering).
2. **Open chat** — Fetches only missed messages via `GET /chats/:id?afterId=<lastCachedMsgId>` and appends them to `pages[0]`.
3. **Fallback** — If too many missed messages (`hasMore = true`) or fetch fails, falls back to `invalidateQueries(['selected_chat', id])` for a full refetch.

### Message Deduplication
All message insertions into the selected chat cache check for existing `message.id` before appending. This prevents duplicates when:
- A WebSocket `receive-message` event and a sync HTTP response deliver the same message.
- Multiple reconnections fire in quick succession.

### Socket.IO Connection
- Socket is created in `services/realtime/socket.js` and stored in Zustand (`useUserStore.socket`).
- Socket.IO handles reconnection automatically (built-in).
- Auth token refresh on `connect_error` with "Unauthorized" is handled in `socket.js`.

## Architectural Mandates

### 1. Data Fetching (TanStack Query)
- **Query Keys:** Centralize query keys in a factory or follow a strict `['entity', filters/id]` pattern. Always pluralize base keys (e.g., `['products']`).
- **Mutations:** Always implement `onSuccess` handlers to invalidate relevant queries (e.g., `queryClient.invalidateQueries({ queryKey: ['products'] })`).
- **Loading States:** Use `PageLoadingOverlay` for full-page transitions and `ButtonLoading` for form submissions.

### 2. Component Design
- **Feature Folders:** Organize by domain in `src/features/`. Each folder should contain its own components, hooks, and services.
- **Reusable UI:** Atomic components live in `src/components/ui/` (Shadcn) or `src/components/common/`.
- **Navigation:** Use `BackButton` in all detail views (`/orders/:id`, `/products/:id`) for UX consistency.

### 3. Styling & Theming
- **Utilities:** Use Tailwind classes exclusively. Avoid inline styles or raw CSS files where possible.
- **Conditional Classes:** Always use the `cn(...)` utility from `lib/utils.js` for merging Tailwind classes.

### 4. Error Handling
- **Toasts:** Use the `toastiify(type, message)` utility from `common/toastiify`.
- **API Errors:** Use the centralized `handleErrors(error, setError)` utility to map backend validation errors to form fields.

## Development Workflow
- **Linting:** Strict ESLint rules for hook dependencies and unused imports.
- **Environment:** API URL is configured via `.env` (Vite prefix `VITE_`).

