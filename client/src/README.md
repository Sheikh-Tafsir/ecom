# Source Structure

- app: root application shell and app-level styles.
- components/ui: reusable design-system primitives.
- components/common: reusable app components shared across pages.
- pages: route-level screens, grouped by feature area.
- providers: React context providers and provider hooks.
- routes: route guard/layout components used by React Router.
- services: external integrations and app-wide clients.
- store: global client state.
- utils: framework-agnostic helpers, constants, and formatting utilities.

Use the @ alias for cross-folder imports. Keep relative imports for files in the same feature folder.
