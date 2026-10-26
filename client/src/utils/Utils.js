export const APP_NAME = "Visored";

export const ONE_MB = 1024 * 1024;
export const MAX_FILE_SIZE = 2 * ONE_MB;

export const validateFile = (file) => {
    return file && file.size <= MAX_FILE_SIZE;
}
