const express = require('express');
const AsyncHandler = require('express-async-handler');

const AuthenticationMiddleware = require('../middleware/AuthenticationMiddleware');
const ValidateNumericParams = require('../middleware/ValidateNumericParams');

const ChatService = require('../service/ChatService');
const {FOUND} = require("../utils/Messages");
const {ok, created} = require("../utils/ResponseUtils");

const router = express.Router();

// /chats

router.get("", AuthenticationMiddleware, AsyncHandler(async (req, res) => {
    const data = await ChatService.findAllChatsByUserId(req?.query, req.user?.id);
    ok(res, {message: FOUND, data})
}));

router.get("/:id", ValidateNumericParams('id'), AuthenticationMiddleware, AsyncHandler(async (req, res) => {
    const data = await ChatService.findDetailsChatById(req.params?.id, req?.query, req.user?.id);
    ok(res, {message: FOUND, data})
}));

router.post("/:id/view", ValidateNumericParams('id'), AuthenticationMiddleware, AsyncHandler(async (req, res) => {
    await ChatService.seenChatMessage(req.params?.id, req.body, req.user?.id)
    created(res, {message: FOUND})
}));

module.exports = router;