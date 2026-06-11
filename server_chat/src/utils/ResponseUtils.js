const ApiResponse = require("../common/ApiResponse");

const ok = (res, {message, data}) => {
    res.status(200).json(ApiResponse({message, data}));
}

const created = (res, {message, data}) => {
    res.status(201).json(ApiResponse({message, data}));
}

const errorResponse = (res, errorMessage, statusCode = 500) => {
    return res.status(statusCode).json(buildErrorResponse(errorMessage));
}

const buildErrorResponse = (errorMessage) => {
    const errors = {
        "global": errorMessage
    }

    return ApiResponse({errors})
}

const multipleErrorResponse = (res, err) => {
    const errors = err.errors.reduce((acc, error) => {
        if (!acc[error.path]) {
            acc[error.path] = error.message;
        }
        return acc;
    }, {});

    return res.status(422).json(ApiResponse({errors}));
}

module.exports = {
    ok,
    created,
    errorResponse,
    buildErrorResponse,
    multipleErrorResponse,
};
