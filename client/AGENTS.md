# Client — Agent Instructions

> For system design, API contract, and deployment see the [shared docs](../docs/).

## What This Service Does

React 18 / Vite SPA — the user-facing frontend. Served by Nginx in production. Communicates exclusively with the Gateway (never directly with `server` or `chat-server`).

## Project Structure

```
src/
├── features/<domain>/          ← feature folders (components, hooks, services per domain)
├── components/
│   ├── ui/                     ← Shadcn atomic components
│   └── common/                 ← shared reusable components
├── services/
│   └── realtime/socket.js      ← Socket.IO instance (stored in Zustand)
└── lib/utils.js                ← cn() utility for Tailwind class merging
```

## Non-Negotiable Rules

- **Feature folders** — organize by domain in `src/features/`. Each folder owns its components, hooks, and services. No cross-domain imports unless going through `components/common/`.
- **Tailwind only** — use Tailwind classes exclusively. No inline styles or raw CSS files.
- **`cn(...)`** — always use for conditional/merged Tailwind classes. Never string concatenation.
- **No direct API calls in components** — data fetching belongs in hooks inside `features/<domain>/hooks/`.
- **`BackButton`** — required in all detail/drill-down views (`/orders/:id`, `/products/:id`, etc.).

## State Management

| Concern         | Tool                  | Rule                                          |
|-----------------|-----------------------|-----------------------------------------------|
| Global state    | Zustand               | `useUserStore`, socket stored here            |
| Server state    | TanStack Query v5     | All server data via `useQuery` / `useInfiniteQuery` |
| Form state      | React Hook Form       | Local to each form component                  |

## React Query Rules

- **Query keys:** `['entity', filters/id]` pattern. Always pluralize base keys (`['products']`, `['orders']`).
- **Mutations:** always implement `onSuccess` with `queryClient.invalidateQueries(...)` for relevant keys.
- **Loading states:** `PageLoadingOverlay` for full-page transitions; `ButtonLoading` for form submits.
- Query key documentation: see [docs/decisions/005-react-query-cache.md](../docs/decisions/005-react-query-cache.md).

## Chat Hook Responsibilities

| Hook               | Purpose                                                              |
|--------------------|----------------------------------------------------------------------|
| `useChatData.js`   | Fetch chat list + selected chat messages (infinite query)            |
| `useChatSync.js`   | WS listeners, reconnect sync, message dedup, mark-as-seen           |
| `useChatActions.js`| Send message, group management (emits WS events)                    |

## Error Handling

- **Toasts:** `toastiify(type, message)` from `common/toastiify` — never raw `alert()`.
- **Form errors:** `handleErrors(error, setError)` maps backend validation errors to form fields.
- **Socket reconnect:** handled automatically by Socket.IO. On `connect_error` with "Unauthorized", `socket.js` refreshes the access token.

## Build & Environment

- **Dev:** `npm run dev` (Vite dev server on port 5173)
- **Build:** `npm run build` (outputs to `dist/`, baked with Vite env vars)
- **Env vars** must use `VITE_` prefix. Baked in at build time — not runtime.
- **Linting:** strict ESLint rules for hook dependencies and unused imports. Fix all warnings before committing.

## Key Env Vars (Vite Build Args)

| Variable                       | Purpose                       |
|--------------------------------|-------------------------------|
| `VITE_API_PATH`                | Gateway base URL              |
| `VITE_GOOGLE_LOGIN_CLIENT_ID`  | Google OAuth client ID        |
| `VITE_SESSION_HINT_KEY`        | Session storage key           |
| `VITE_LOCAL_STORAGE_CART_KEY`  | Cart persistence key          |
| `VITE_WEB_SOCKET_ON`           | Enable/disable Socket.IO      |
| `VITE_SSE_ON`                  | Enable/disable SSE            |
