import { useEffect, useState } from "react";

import { API_PATH } from "@/services/http/Axios.js";
import { Notification } from "./Notification";
import { getAccessToken } from "@/utils/AuthUtils";

export default function NotificationListener() {
  const token = getAccessToken();
  const [alerts, setAlerts] = useState([]);

  // useEffect(() => {
  //   if (!token) return;

  //   let eventSource = new EventSource(`${API_PATH}/notifications?token=${token}`);

  //   eventSource.onmessage = (e) => {
  //     const data = JSON.parse(e.data);
  //     //console.log(data);
  //     setAlerts((prev) => [
  //       ...prev,
  //       {
  //         id: Date.now(),
  //         ...data,
  //       },
  //     ]);
  //   };

  //   eventSource.onerror = (err) => {
  //     //console.error(err);
  //     eventSource.close();
  //   };

  //   return () => eventSource.close();
  // }, []);

  const removeAlert = (id) => {
    setAlerts((prev) => prev.filter((a) => a.id !== id));
  };

  return (
    <div className="fixed top-20 right-4 flex flex-col gap-2 z-10">
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
