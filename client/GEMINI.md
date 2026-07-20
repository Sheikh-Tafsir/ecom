# Client Module (Frontend)

## Overview
A modern React application built with Vite and Tailwind CSS.

## Tech Stack
- **Framework:** React 18+ (JSX), Vite.
- **Styling:** Tailwind CSS, Shadcn/ui.
- **State:** Zustand (Global), React Hook Form (Local Forms).
- **Data:** TanStack Query (React Query) v5+.

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
