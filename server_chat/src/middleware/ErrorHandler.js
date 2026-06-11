const {SOMETHING_WENT_WRONG, NOT_FOUND, FORBIDDEN_ACCESS} = require("../utils/Messages");
const {errorResponse, multipleErrorResponse} = require("../utils/ResponseUtils");

const ErrorHandler = (err, req, res, next) => {
    console.error('ErrorHandler caught:', err);

    res.set({
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        'Pragma': 'no-cache',
        'Expires': '0'
    });

    if (err.status === 400) {
        return errorResponse(res, err.message, err.status)
    }

    if (err.status === 401) {
        return errorResponse(res, "Authentication Required", err.status)
    }

    if (err.status === 403) {
        return errorResponse(res, FORBIDDEN_ACCESS, err.status)
    }

    if (err.status === 404) {
        return errorResponse(res, NOT_FOUND, err.status)
    }

    if (err.status === 422) {
        return errorResponse(res, err.message, err.status)
    }

    if (err.errors && Array.isArray(err.errors)) {
        return multipleErrorResponse(res, err)
    }

    return errorResponse(res, SOMETHING_WENT_WRONG, err.status)
};

module.exports = ErrorHandler 