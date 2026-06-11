const { ApiMessageResponse } = require("../utils/Utils");

const ValidateParamsIdMatch = (req, res, next) => {
    if (req.body.id && req.params.id != req.body.id) {
        console.log("ID mismatch between path and body");
        return res.status(400).json(ApiMessageResponse("URL parameter ID does not match request body ID."));
    }
    next();
};

module.exports = ValidateParamsIdMatch;
