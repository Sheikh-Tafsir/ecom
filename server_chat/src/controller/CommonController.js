const express = require('express');
const AsyncHandler = require('../middleware/AsyncHandler');
const AuthenticationMiddleware = require('../middleware/AuthenticationMiddleware');
const Repository = require('../common/Repository');
const FileUpload = require("../middleware/FileUpload");
const cloudinary = require('cloudinary').v2;
const {ok} = require('../utils/ResponseUtils');

const router = express.Router();

// /common

/**
 * @swagger
 * /common/upload:
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
router.post("/upload", AuthenticationMiddleware, FileUpload, AsyncHandler(async (req, res) => {
    res.status(200).json(await Repository.uploadImage(req.file));
}));

/**
 * @swagger
 * /common/upload-signature:
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
router.post("/upload-signature", AuthenticationMiddleware, AsyncHandler(async (req, res) => {
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
