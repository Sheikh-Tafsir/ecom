const express = require('express');
const AsyncHandler = require('../middleware/AsyncHandler');
const AuthenticationMiddleware = require('../middleware/AuthenticationMiddleware');
const Repository = require('../common/Repository');
const FileUpload = require("../middleware/FileUpload");

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
    res.status(200).json(await Repository.uploadImage(req.files?.image));
}));

module.exports = router;
