const RuntimeError = require("../common/RuntimeError");
const {NOT_FOUND} = require("../utils/Messages");

const NotFoundHandler = (req, res, next) => {
    next(new RuntimeError(404, NOT_FOUND));
};

module.exports = NotFoundHandler;
