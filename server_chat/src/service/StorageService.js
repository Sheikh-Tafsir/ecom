const { uploadToCloudinary, generateUploadSignature } = require('../config/CloudinaryConfig');
const { reduceImageBySize } = require('../utils/ImageUtils');
const RuntimeError = require('../common/RuntimeError');
const { logger } = require('../config/logger');

/**
 * StorageService — Abstract File & Image Storage Layer
 *
 * Encapsulates file compression, direct uploads, and signature generation,
 * isolating controllers from underlying storage SDKs (Cloudinary, S3, etc.).
 */
class StorageService {
    /**
     * Upload an image buffer with automated size optimization.
     * @param {Buffer} buffer - Raw file buffer
     * @param {string} [folder] - Optional destination folder
     * @returns {Promise<{ url: string, secure_url: string, publicId: string }>}
     */
    async uploadChatImage(buffer, folder) {
        if (!buffer || !Buffer.isBuffer(buffer)) {
            throw new RuntimeError(400, "Invalid image file buffer.");
        }

        try {
            const reducedBuffer = await reduceImageBySize(buffer);
            const result = await uploadToCloudinary(reducedBuffer, folder);
            return {
                url: result.url,
                secure_url: result.secure_url,
                publicId: result.public_id,
            };
        } catch (err) {
            logger.error('StorageService uploadChatImage failed:', err.message);
            throw new RuntimeError(500, "Failed to upload image. Please try again.");
        }
    }

    /**
     * Generate signed upload parameters for direct client uploads.
     * @param {string} [folder]
     * @returns {Object}
     */
    generateUploadSignature(folder = 'chat-images') {
        try {
            return generateUploadSignature(folder);
        } catch (err) {
            logger.error('StorageService generateUploadSignature failed:', err.message);
            throw new RuntimeError(500, "Failed to generate upload signature.");
        }
    }
}

module.exports = new StorageService();
