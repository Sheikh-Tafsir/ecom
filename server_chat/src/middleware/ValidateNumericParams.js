const { ApiMessageResponse } = require("../utils/Utils");

module.exports = function ValidateNumericParams(...paramNames) {
    return (req, res, next) => {
      for (const name of paramNames) {
        const rawValue = req.params[name];
        if (!/^\d+$/.test(rawValue)) {
          return res.status(400).json(ApiMessageResponse(`${name} must be a valid numeric integer.`));
        }
        req.params[name] = parseInt(rawValue, 10);
      }
      next();
    };
  };
  