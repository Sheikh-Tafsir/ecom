import { API_PATH, refreshAccessToken } from "@/services/http/Axios.js";
import { getAccessToken } from "@/utils/AuthUtils";
import { useNotificationStore } from "@/store/useNotificationStore.js";

export const isSseOn = () => import.meta.env.VITE_SSE_ON === "true";

class NotificationService {
  constructor() {
    this.eventSource = null;
    this.token = getAccessToken();
  }

  start(token = getAccessToken()) {
    if (!token || !isSseOn()) return;

    this.token = token;
    this.connect();
  }

  connect() {
    if (this.eventSource) {
      this.eventSource.close();
    }

    this.eventSource = new EventSource(`${API_PATH}/notifications/subscribe?accessToken=${this.token}`);

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
      }
      
      try {
        const newToken = await refreshAccessToken();
        this.token = newToken;
        this.connect();
      } catch (refreshErr) {
        console.error("Failed to refresh token for SSE", refreshErr);
      }
    };
  }

  stop() {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
}

export const notificationService = new NotificationService();
