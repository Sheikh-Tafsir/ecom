import { format } from "date-fns";

export const REGULAR_DATE_FORMAT = "dd-MM-yyyy";

export const INVERSE_DATE_FORMAT = "yyyy-MM-dd";

export const REGULAR_TIME_FORMAT_12 = "hh:mm a";

export const REGULAR_TIME_FORMAT_24 = "HH:mm";

export const formatDateString = (dateStr) => {
    if (!dateStr) return "-";

    // convert "2026-06-04 22:21:46.170915" → "2026-06-04T22:21:46"
    const normalized = dateStr.replace(" ", "T").split(".")[0];

    const date = new Date(normalized);

    if (isNaN(date.getTime())) return "-";

    const day = String(date.getDate()).padStart(2, "0");
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const year = date.getFullYear();

    return `${day}-${month}-${year}`;
};

export const isSameDay = (date1, date2) => {
    return date1.getFullYear() == date2.getFullYear() &&
        date1.getMonth() == date2.getMonth() &&
        date1.getDate() == date2.getDate();
}

export const isYesterday = (date, today) => {
    const yesterday = new Date(today);
    yesterday.setDate(today.getDate() - 1);
    return isSameDay(date, yesterday);
};

export const getLastMessageTime = (timestamp) => {
    // Add 6 hours to the original timestamp
    const messageDate = new Date(new Date(timestamp).getTime() + 6 * 60 * 60 * 1000);
    const now = new Date();

    if (isSameDay(messageDate, now)) {
        return format(messageDate, REGULAR_TIME_FORMAT_24);
    }

    if (isYesterday(messageDate, now)) {
        return `Yesterday ${format(messageDate, REGULAR_TIME_FORMAT_24)}`;
    }

    return format(messageDate, REGULAR_DATE_FORMAT);
};