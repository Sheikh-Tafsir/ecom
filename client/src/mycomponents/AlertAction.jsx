import React from "react";

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import {
  Trash2,
  CircleCheck,
  Pencil,
  CircleUser,
  CirclePlus,
} from "lucide-react";
import { ALERT_TYPE } from "@/utils/enums";

const ICON_BUTTON_PROP = {
  variant: "outline",
  size: "icon",
};

const ICON_BUTTON_CLASS = "h-4 w-4";

const getTriggerButton = (type, icon, css) => {
  if (icon) {
    switch (type) {
      case ALERT_TYPE.ADD:
        return (
          <Button {...ICON_BUTTON_PROP}>
            <CirclePlus className={ICON_BUTTON_CLASS} />
          </Button>
        );
      case ALERT_TYPE.CONFIRM:
        return (
          <Button
            {...ICON_BUTTON_PROP}
            className="text-green-600 hover:bg-green-600 hover:text-white"
          >
            <CircleCheck className={ICON_BUTTON_CLASS} />
          </Button>
        );
      case ALERT_TYPE.EDIT:
        return (
          <Button
            {...ICON_BUTTON_PROP}
            className="text-blue-600 hover:bg-blue-600 hover:text-white"
          >
            <Pencil className={ICON_BUTTON_CLASS} />
          </Button>
        );
      case ALERT_TYPE.DELETE:
        return (
          <Button
            {...ICON_BUTTON_PROP}
            className="text-red-600 hover:bg-red-600 hover:text-white"
          >
            <Trash2 className={ICON_BUTTON_CLASS} />
          </Button>
        );
      default:
        return (
          <Button {...ICON_BUTTON_PROP}>
            <CircleUser className={ICON_BUTTON_CLASS} />
          </Button>
        );
    }
  }

  // fallback if no icon
  return (
    <Button variant="destructive" className={css}>
      Delete
    </Button>
  );
};

const getDescription = (type) => {
  if (type === ALERT_TYPE.DELETE) {
    return "This action cannot be undone. It will permanently delete the item.";
  }
  return "";
};

const getStyle = (type) => {
  if (type == ALERT_TYPE.DELETE) return "bg-red-600 hover:bg-red-700";
  else ""
}
// Alert choose to proceed or cancel
export function AlertAction({ onConfirm, description, type, icon, css }) {
  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        {getTriggerButton(type, icon, css)}
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle />
          <AlertDialogDescription>
            {description || getDescription(type)}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancel</AlertDialogCancel>
          <AlertDialogAction onClick={onConfirm} className={getStyle(type)}>
            {type}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
