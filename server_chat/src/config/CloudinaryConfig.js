const cloudinary = require('cloudinary').v2;
const { CLOUDINARY_CHAT_FOLDER } = require('../constants/chat.constants');

require('dotenv').config();

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

/**
 * Upload buffer to Cloudinary using stream uploader.
 * @param {Buffer} buffer
 * @param {string} [folder]
 * @returns {Promise<Object>}
 */
const uploadToCloudinary = async (buffer, folder = CLOUDINARY_CHAT_FOLDER) => {
  return new Promise((resolve, reject) => {
    const stream = cloudinary.uploader.upload_stream(
      { folder },
      (error, result) => {
        if (error) return reject(error);
        resolve(result);
      }
    );
    const readable = require('stream').Readable.from(buffer);
    readable.pipe(stream);
  });
};

/**
 * Generate cryptographic signature for direct client-to-Cloudinary uploads.
 * @param {string} [folder]
 * @returns {Object}
 */
const generateUploadSignature = (folder = 'chat-images') => {
  const timestamp = Math.round(Date.now() / 1000);
  const params = { timestamp, folder };
  const signature = cloudinary.utils.api_sign_request(
    params,
    process.env.CLOUDINARY_API_SECRET
  );

  return {
    signature,
    timestamp,
    folder,
    cloudName: process.env.CLOUDINARY_CLOUD_NAME,
    apiKey: process.env.CLOUDINARY_API_KEY,
  };
};

module.exports = {
  uploadToCloudinary,
  generateUploadSignature,
};
