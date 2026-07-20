import {toast} from "react-toastify";

const TOAST_METHODS = ['success', 'info', 'warn', 'error'];

export const toastify = (type, message) => {
    if (TOAST_METHODS.includes(type)) {
        toast[type](message);
    } else {
        toast(message);
    }
};