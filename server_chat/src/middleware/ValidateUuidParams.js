const { ApiMessageResponse } = require("../utils/Utils");
const { isValidUuid } = require("../utils/UuidUtils");

module.exports = function ValidateUuidParams(...paramNames) {
    return (req, res, next) => {
        for (const name of paramNames) {
            const rawValue = req.params[name];
            if (!isValidUuid(rawValue)) {
                return res.status(400).json(ApiMessageResponse(`${name} must be a valid UUID.`));
            }
        }
        next();
    };
};
