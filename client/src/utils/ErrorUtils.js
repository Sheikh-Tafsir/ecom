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
        }

        if (data?.message) {
            handleGlobalError(data.message, setError);
        }

        return;
    }

    if (error?.message == NETWORK_ERROR || error?.request) {
        handleGlobalError(NETWORK_ERROR, setError);
        return;
    }

    handleGlobalError(error?.message || "Unexpected error occurred.", setError);
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

const handleGlobalError = (message, setError) => {
    setError?.(GLOBAL_ERROR, {
        type: "client",
        message,
    });
};

const extractMessage = (message) => {
    if (Array.isArray(message)) return message[0];
    if (typeof message === "string") return message;
    return JSON.stringify(message);
};