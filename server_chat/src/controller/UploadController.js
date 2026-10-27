const express = require('express');
const AsyncHandler = require('../middleware/AsyncHandler');
const AuthenticationMiddleware = require('../middleware/AuthenticationMiddleware');
const UploadMiddleware = require("../middleware/UploadMiddleware");
const cloudinary = require('cloudinary').v2;
const { reduceImageBySize } = require('../utils/ImageUtils');
const { uploadToCloudinary } = require('../config/CloudinaryConfig');
const RuntimeError = require('../common/RuntimeError');
const {ok} = require('../utils/ResponseUtils');
const {FOUND} = require('../utils/Messages');

const router = express.Router();

// /common

/**
 * @swagger
 * /uploads:
 *   post:
 *     summary: Upload an image
 *     tags: [Common]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       content:
 *         multipart/form-data:
 *           schema:
 *             type: object
 *             properties:
 *               image:
 *                 type: string
 *                 format: binary
 *     responses:
 *       200:
 *         description: Image uploaded successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 url:
 *                   type: string
 */
router.post("", AuthenticationMiddleware, UploadMiddleware, AsyncHandler(async (req, res) => {
    if (!req.file) {
        throw new RuntimeError(400, "Image not uploaded");
    }

    const reducedBuffer = await reduceImageBySize(req.file.buffer);
    const uploaded = await uploadToCloudinary(reducedBuffer);
    ok(res, { message: FOUND, data: uploaded.secure_url });
}));

/**
 * @swagger
 * /uploads/signature:
 *   post:
 *     summary: Generate a Cloudinary signed upload signature for direct client uploads
 *     tags: [Common]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               folder:
 *                 type: string
 *                 example: chat-images
 *     responses:
 *       200:
 *         description: Upload signature generated
 */
router.post("/signature", AuthenticationMiddleware, AsyncHandler(async (req, res) => {
    const folder = req.body?.folder || 'chat-images';
    const timestamp = Math.round(Date.now() / 1000);

    const params = { timestamp, folder };
    const signature = cloudinary.utils.api_sign_request(
        params,
        process.env.CLOUDINARY_API_SECRET
    );

    ok(res, {
        message: 'Upload signature generated',
        data: {
            signature,
            timestamp,
            folder,
            cloudName: process.env.CLOUDINARY_CLOUD_NAME,
            apiKey: process.env.CLOUDINARY_API_KEY,
        },
    });
}));

module.exports = router;
