const multer = require('multer');
const { allowedImageTypes } = require('../utils/ImageUtils');
const MAX_SIZE = 5 * 1024 * 1024;

const FileUpload = multer({
  // dest: 'uploads/',
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

module.exports = FileUpload