const cloudinary = require('cloudinary').v2;
const { CLOUDINARY_CHAT_FOLDER } = require('../constants/chat.constants');

require('dotenv').config()

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

const uploadToCloudinary = async (buffer) => {
  return new Promise((resolve, reject) => {
    const stream = cloudinary.uploader.upload_stream(
      { folder: CLOUDINARY_CHAT_FOLDER },
      (error, result) => {
        if (error) return reject(error);
        resolve(result);
      }
    );
    // stream.end(buffer);
    const readable = require('stream').Readable.from(buffer);
    readable.pipe(stream);
  });
};


module.exports = { uploadToCloudinary };