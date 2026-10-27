const multer = require('multer');
const { allowedImageTypes } = require('../utils/ImageUtils');
const RuntimeError = require('../common/RuntimeError');
const MAX_SIZE = 5 * 1024 * 1024;

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: MAX_SIZE },
  fileFilter: (req, file, cb) => {
    const mimetype = file.mimetype;
    if (mimetype.startsWith('image/') && allowedImageTypes.includes(mimetype)) {
      cb(null, true);
    } else {
      cb(new RuntimeError(422, 'file type not allowed!'));
    }
  },
});

// Export the single-file middleware so it can be used directly in route definitions
module.exports = upload.single('image');
