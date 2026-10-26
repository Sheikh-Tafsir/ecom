const { hasValue } = require("../utils/Utils");

const ApiResponse = ({message, data, errors}) => {
    const response = {};

    if (hasValue(message)) response.message = message;
    if (hasValue(data)) response.data = data;
    if (hasValue(errors)) response.error = errors;

    return response;
};

module.exports = ApiResponse;
