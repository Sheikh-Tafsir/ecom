const multer = require('multer');
const {SOMETHING_WENT_WRONG, NOT_FOUND, FORBIDDEN_ACCESS} = require("../utils/Messages");
const {errorResponse, multipleErrorResponse} = require("../utils/ResponseUtils");
const {logger} = require("../config/logger");

const REQUEST_ID_HEADER = "X-Request-Id";

// eslint-disable-next-line no-unused-vars -- Express requires 4-param signature for error-handling middleware
const ErrorHandler = (err, req, res, next) => {
    const requestId = req.header(REQUEST_ID_HEADER);
    logger.error({ requestId, err }, 'ErrorHandler caught');

    res.set({
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        'Pragma': 'no-cache',
        'Expires': '0'
    });

    // Multer file validation errors (size limit, unexpected field, etc.)
    if (err instanceof multer.MulterError) {
        const message = err.code === 'LIMIT_FILE_SIZE'
            ? 'File size exceeds the maximum allowed limit'
            : err.message;
        return errorResponse(res, message, 422);
    }

    if (err.status === 400) {
        return errorResponse(res, err.message, err.status)
    }

    if (err.status === 401) {
        return errorResponse(res, err.message || "Authentication Required", err.status)
    }

    if (err.status === 403) {
        return errorResponse(res, err.message || FORBIDDEN_ACCESS, err.status)
    }

    if (err.status === 404) {
        return errorResponse(res, NOT_FOUND, err.status)
    }

    if (err.status === 422) {
        return errorResponse(res, err.message, err.status)
    }

    if (err.status === 429) {
        return errorResponse(res, err.message || "Too many requests. Please slow down.", 429)
    }

    if (err.status === 503) {
        return errorResponse(res, err.message || SOMETHING_WENT_WRONG, err.status)
    }

    if (err.errors && Array.isArray(err.errors)) {
        return multipleErrorResponse(res, err)
    }

    return errorResponse(res, SOMETHING_WENT_WRONG, err.status || 500)
};

module.exports = ErrorHandler
