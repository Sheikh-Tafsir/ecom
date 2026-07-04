import { useEffect, useState } from "react";
import { X } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { TOAST_TYPE } from "@/utils/enums";

export function Notification({ notification, onClose }) {
  const [isOpen, setIsOpen] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setIsOpen(false);
      onClose();
    }, 5000); // auto-close after 5s

    return () => clearTimeout(timer);
  }, [onClose]);

  const colorStyle =
    notification.type == TOAST_TYPE.ERROR
      ? "bg-red-500"
      : notification.type == TOAST_TYPE.WARNING
        ? "bg-red-400"
        : notification.type == TOAST_TYPE.SUCCESS
          ? "bg-green-500"
          : "bg-blue-500";

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -10 }}
          transition={{ duration: 0.2 }}
        >
          <Alert className={`w-[250px] text-white flex justify-between px-4 rounded-lg shadow-md ${colorStyle}`}>
            <div className="my-auto">
              <AlertDescription>{notification.message}</AlertDescription>
            </div>
            <Button
              size="icon"
              variant="outline"
              className="my-auto w-6 h-6"
              onClick={() => {
                setIsOpen(false);
                onClose();
              }}
            >
              <X className="text-blue-600" />
            </Button>
          </Alert>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
