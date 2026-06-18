const NETWORK_ERROR = "Network Error";

export const GLOBAL_ERROR = "global";

export const getError = (errors, field) => {
    return errors?.[field]?.message;
};

export const handleErrors = (error, setError) => {
    if (error?.response?.data) {
        const data = error.response.data;

        const errors = data?.errors;

        if (errors && typeof errors === "object") {
            handleFieldErrors(errors, setError);
            return;
        }

        if (data?.message) {
            handleClientSideGlobalError(data.message, setError);
        }

        return;
    }

    if (error?.message == NETWORK_ERROR || error?.request) {
        handleClientSideGlobalError(NETWORK_ERROR, setError);
        return;
    }

    handleClientSideGlobalError(error?.message || "Unexpected error occurred.", setError);
};

const handleFieldErrors = (errors, setError) => {
    Object.entries(errors).forEach(([field, messages]) => {
        const message = extractMessage(messages);

        setError(field, {
            type: "server",
            message
        });
    });
};

export const handleClientSideGlobalError = (message, setError) => {
    handleClientSideError(GLOBAL_ERROR, message, setError)
};

export const handleClientSideError = (field, message, setError) => {
    setError?.(field, {
        type: "client",
        message,
    });
};

const extractMessage = (message) => {
    if (Array.isArray(message)) {
        return message[0];
    }
    if (typeof message === "string") return message;
    return JSON.stringify(message);
};