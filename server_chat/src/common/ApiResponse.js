const {isNull} = require("../utils/Utils");

const ApiResponse = ({message, data, errors}) => {
    const response = {};

    if (!isNull(message)) response.message = message;
    if (!isNull(data)) response.data = data;
    if (!isNull(errors)) response.error = errors;


    return response;
};

module.exports = ApiResponse;