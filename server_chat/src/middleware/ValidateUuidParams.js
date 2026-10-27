const { isValidUuid } = require("../utils/UuidUtils");
const RuntimeError = require("../common/RuntimeError");

module.exports = function ValidateUuidParams(...paramNames) {
    return (req, res, next) => {
        for (const name of paramNames) {
            const rawValue = req.params[name];
            if (!isValidUuid(rawValue)) {
                return next(new RuntimeError(400, `${name} must be a valid UUID.`));
            }
        }
        next();
    };
};
