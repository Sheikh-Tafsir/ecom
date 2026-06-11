const { ApiMessageResponse } = require("../utils/Utils");

module.exports = function ValidateNumericParams(...paramNames) {
    return (req, res, next) => {
      for (const name of paramNames) {
        const value = parseInt(req.params[name]);
        // console.log(value);
        if (isNaN(value)) {
          return res.status(400).json(ApiMessageResponse(`${name} must be a number.`));
        }
      }
      next();
    };
  };
  