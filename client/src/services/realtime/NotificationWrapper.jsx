import { useNotificationStore } from "@/store/useNotificationStore.js";
import { Notification } from "@/components/common/Notification.jsx";

export default function NotificationWrapper() {
  const alerts = useNotificationStore((state) => state.alerts);
  const removeAlert = useNotificationStore((state) => state.removeAlert);

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
