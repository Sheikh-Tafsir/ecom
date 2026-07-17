import { useEffect, useState } from "react";

import { API_PATH, refreshAccessToken } from "@/services/http/Axios.js";
import { Notification } from "../../components/common/Notification";
import { getAccessToken } from "@/utils/AuthUtils";

export default function NotificationListener() {
  const [token, setToken] = useState(getAccessToken());
  const [alerts, setAlerts] = useState([]);

  useEffect(() => {
    const isSseOn = import.meta.env.VITE_SSE_ON == "true";
    if (!token || !isSseOn) return;

    let eventSource = new EventSource(`${API_PATH}/notifications/subscribe?accessToken=${token}`);

    eventSource.addEventListener("init", (e) => {
      //console.log("SSE Initialized:", e.data);
      //  try {
      //   const data = JSON.parse(e.data);
      //   setAlerts((prev) => [
      //     ...prev,
      //     {
      //       id: Date.now(),
      //       type: data.type.toLowerCase(),
      //       message: data.message,
      //     },
      //   ]);
      // } catch (err) {
      //   console.error("Failed to parse notification data", err);
      // }
    });

    eventSource.addEventListener("notification", (e) => {
      //console.log("Notification received:", e.data);
      try {
        const data = JSON.parse(e.data);
        setAlerts((prev) => [
          ...prev,
          {
            id: Date.now(),
            type: data.type,
            message: data.message,
          },
        ]);
      } catch (err) {
        console.error("Failed to parse notification data", err);
      }
    });

    eventSource.addEventListener("heartbeat", (e) => {
      //console.log("Heartbeat received");
    });

    eventSource.onerror = async (err) => {
      //console.warn("SSE Error, attempting to refresh token...", err);
      eventSource.close();

      try {
        const newToken = await refreshAccessToken();
        setToken(newToken);
      } catch (refreshErr) {
        console.error("Failed to refresh token for SSE", refreshErr);
      }
    };

    return () => eventSource.close();
  }, [token]);

  const removeAlert = (id) => {
    setAlerts((prev) => prev.filter((a) => a.id !== id));
  };

  return (
    <div className="fixed top-24 right-2 flex flex-col gap-2 z-10">
      {alerts.map((alert) => (
        <Notification
          key={alert.id}
          notification={alert}
          onClose={() => removeAlert(alert.id)}
        />
      ))}
    </div>
  );
}
