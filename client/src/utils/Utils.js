import { USER_ROLE } from "./enums";

export const APP_NAME = "Visored";

export const MAX_FILE_SIZE = 2 * 1024 * 1024;

export const isNull = (value) => {
    return !value || value == null || value == undefined || value == "";
}

export const isNotNull = (value) => {
    return !isNull(value);
}

export const isInRange = (value, min, max) => {
    return value >= min && value <= max;
}

export const isAdmin = (role) => {
    return USER_ROLE.ADMIN === role || USER_ROLE.SUPER_ADMIN === role;
}

export const initialToastState = {
    message: "",
    type: "",
    id: Date.now(),
};

export const imageToByte = (file) => {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = (error) => reject(error);
        reader.readAsDataURL(file);
    });
};

export const validateFile = (file) => {
    return file && file.size <= MAX_FILE_SIZE;
}

export const capitalizeFirstLetter = (string) => {
    if (!string) return "";
    return string.charAt(0).toUpperCase() + string.slice(1);
}

export const prepareMultipartForm = (data) => {
    const formData = new FormData();

    Object.entries(data).forEach(([key, value]) => {
        if (value != null && key !== "image") formData.append(key, value);
    });

    if (data.image instanceof File) {
        formData.append('image', data.image);
    }

    return formData;
};
