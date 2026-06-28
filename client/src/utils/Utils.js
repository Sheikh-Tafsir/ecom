export const APP_NAME = "Visored";

export const ONE_MB = 1024 * 1024;
export const MAX_FILE_SIZE = 2 * ONE_MB;

export const isEmptyArray = (arr) => !arr || arr.length === 0;

export const isInRange = (value, min, max) => {
    return value >= min && value <= max;
}

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
        if (key === "image") return;

        if (Array.isArray(value)) {
            value.forEach((item) => formData.append(key, item));
        } else {
            formData.append(key, value ?? "");
        }
    });

    return formData;
};
