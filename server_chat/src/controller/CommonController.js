const express = require('express');
const AsyncHandler = require('../middleware/AsyncHandler');
const AuthenticationMiddleware = require('../middleware/AuthenticationMiddleware');
const Repository = require('../common/Repository');
const FileUpload = require("../middleware/FileUpload");

const router = express.Router();

// /common

router.post("/upload", AuthenticationMiddleware, FileUpload, AsyncHandler(async (req, res) => {
    res.status(200).json(await Repository.uploadImage(req.files?.image));
}));

module.exports = router;
