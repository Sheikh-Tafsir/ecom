const NETWORK_ERROR = "Network error. Please check your internet connection.";

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
    }

    if (error?.request || error?.message === "Network Error") {
        handleGlobalError(NETWORK_ERROR, setError);
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

const handleGlobalError = (error, setError) => {
    setError?.("global", {
        error,
    });
};

const extractMessage = (message) => {
    if (Array.isArray(message)) return message[0];
    if (typeof message === "string") return message;
    return JSON.stringify(message);
};