import { API_PATH, AuthenticatedAxios } from "@/services/http/Axios.js";
import { getAccessToken } from "@/utils/AuthUtils";
import { useNotificationStore } from "@/store/useNotificationStore.js";

export const isSseOn = () => import.meta.env.VITE_SSE_ON === "true";

class NotificationService {
  constructor() {
    this.eventSource = null;
    this.isConnecting = false;
  }

  start() {
    if (!getAccessToken() || !isSseOn()) return;
    this.connect();
  }

  async connect() {
    if (this.isConnecting) return;
    this.isConnecting = true;

    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }

    try {
      const response = await AuthenticatedAxios.get("/notifications/sse-token");
      const ticket = response.data.data;

      if (!this.isConnecting) return;

      this.eventSource = new EventSource(`${API_PATH}/notifications/subscribe?ticket=${ticket}`);

      this.eventSource.addEventListener("notification", (e) => {
        try {
          const data = JSON.parse(e.data);
          useNotificationStore.getState().addAlert({
            type: data.type,
            message: data.message,
          });
        } catch (err) {
          console.error("Failed to parse notification data", err);
        }
      });

      this.eventSource.onerror = async (err) => {
        if (this.eventSource) {
          this.eventSource.close();
          this.eventSource = null;
        }

        setTimeout(() => {
          if (getAccessToken()) {
            this.connect();
          }
        }, 5000);
      };
    } catch (err) {
      console.error("Failed to fetch SSE ticket", err);
      setTimeout(() => {
        if (getAccessToken()) {
          this.connect();
        }
      }, 5000);
    } finally {
      this.isConnecting = false;
    }
  }

  stop() {
    this.isConnecting = false;
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
}

export const notificationService = new NotificationService();
