import { useNotificationStore } from '@/store/useNotificationStore';
import { NOTIFICATION_EVENT } from '@/services/realtime/socketEvents';

export const registerNotificationListeners = (socket) => {
  if (!socket) return () => {};

  const handleNotification = (data) => {
    if (data && data.message) {
      useNotificationStore.getState().addAlert({
        type: data.type,
        message: data.message,
      });
    }
  };

  socket.on(NOTIFICATION_EVENT, handleNotification);

  return () => {
    socket.off(NOTIFICATION_EVENT, handleNotification);
  };
};
