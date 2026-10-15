import { create } from "zustand";

const MAX_ALERTS = 10;

const generateId = () => {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
};

export const useNotificationStore = create((set) => ({
  alerts: [],
  addAlert: (alert) =>
    set((state) => {
      const newAlert = { ...alert, id: alert?.id || generateId() };
      const updated = [...state.alerts, newAlert];
      return { alerts: updated.slice(-MAX_ALERTS) };
    }),
  removeAlert: (id) =>
    set((state) => ({
      alerts: state.alerts.filter((alert) => alert.id !== id),
    })),
}));
