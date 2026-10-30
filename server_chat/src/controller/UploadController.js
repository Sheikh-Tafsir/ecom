const express = require('express');
const AsyncHandler = require('../middleware/AsyncHandler');
const AuthenticationMiddleware = require('../middleware/AuthenticationMiddleware');
const UploadMiddleware = require('../middleware/UploadMiddleware');
const StorageService = require('../service/StorageService');
const RuntimeError = require('../common/RuntimeError');
const { ok } = require('../utils/ResponseUtils');
const { FOUND } = require('../utils/Messages');

const router = express.Router();

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

    const uploaded = await StorageService.uploadChatImage(req.file.buffer);
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
    const signatureData = StorageService.generateUploadSignature(folder);

    ok(res, {
        message: 'Upload signature generated',
        data: signatureData,
    });
}));

module.exports = router;
