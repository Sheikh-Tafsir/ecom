const express = require('express');
const AsyncHandler = require('express-async-handler');

const AuthenticationMiddleware = require('../middleware/AuthenticationMiddleware');
const ValidateNumericParams = require('../middleware/ValidateNumericParams');

const ChatService = require('../service/ChatService');
const {FOUND} = require("../utils/Messages");
const {ok, created} = require("../utils/ResponseUtils");

const router = express.Router();

/**
 * @swagger
 * components:
 *   schemas:
 *     Participant:
 *       type: object
 *       properties:
 *         id:
 *           type: integer
 *         userId:
 *           type: integer
 *         name:
 *           type: string
 *         image:
 *           type: string
 *         role:
 *           type: string
 *     Chat:
 *       type: object
 *       properties:
 *         id:
 *           type: integer
 *         type:
 *           type: string
 *           enum: [direct, group]
 *         name:
 *           type: string
 *         image:
 *           type: string
 *         lastMessage:
 *           type: string
 *         lastSent:
 *           type: string
 *           format: date-time
 *         unreadMessage:
 *           type: integer
 *         participants:
 *           type: array
 *           items:
 *             $ref: '#/components/schemas/Participant'
 *     Message:
 *       type: object
 *       properties:
 *         id:
 *           type: integer
 *         chatId:
 *           type: integer
 *         content:
 *           type: string
 *         contentType:
 *           type: string
 *           enum: [text, image, file]
 *         senderId:
 *           type: integer
 *         createdAt:
 *           type: string
 *           format: date-time
 *     Pagination:
 *       type: object
 *       properties:
 *         hasMore:
 *           type: boolean
 *         nextCursor:
 *           type: integer
 *         limit:
 *           type: integer
 */

// /chats

/**
 * @swagger
 * /chats:
 *   get:
 *     summary: Get all chats for the authenticated user
 *     tags: [Chats]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: type
 *         schema:
 *           type: string
 *           enum: [direct, group]
 *         description: Filter chats by type
 *       - in: query
 *         name: cursorLastSent
 *         schema:
 *           type: string
 *           format: date-time
 *         description: Pagination cursor (lastSent of the last chat in previous page)
 *       - in: query
 *         name: cursorId
 *         schema:
 *           type: integer
 *         description: Pagination cursor (id of the last chat in previous page)
 *     responses:
 *       200:
 *         description: A list of chats
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 message:
 *                   type: string
 *                 data:
 *                   type: object
 *                   properties:
 *                     chats:
 *                       type: array
 *                       items:
 *                         $ref: '#/components/schemas/Chat'
 *                     pagination:
 *                       $ref: '#/components/schemas/Pagination'
 */
router.get("", AuthenticationMiddleware, AsyncHandler(async (req, res) => {
    const data = await ChatService.findAllChatsByUserId(req?.query, req.user?.id);
    ok(res, {message: FOUND, data})
}));

/**
 * @swagger
 * /chats/{id}:
 *   get:
 *     summary: Get detailed chat information including messages
 *     tags: [Chats]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: The chat ID
 *       - in: query
 *         name: cursorCreatedAt
 *         schema:
 *           type: string
 *           format: date-time
 *         description: Pagination cursor (createdAt of the last message in previous page)
 *       - in: query
 *         name: cursorId
 *         schema:
 *           type: integer
 *         description: Pagination cursor (id of the last message in previous page)
 *     responses:
 *       200:
 *         description: Chat details and messages
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 message:
 *                   type: string
 *                 data:
 *                   type: object
 *                   properties:
 *                     id:
 *                       type: integer
 *                     type:
 *                       type: string
 *                     name:
 *                       type: string
 *                     participants:
 *                       type: array
 *                       items:
 *                         $ref: '#/components/schemas/Participant'
 *                     messages:
 *                       type: array
 *                       items:
 *                         $ref: '#/components/schemas/Message'
 *                     pagination:
 *                       $ref: '#/components/schemas/Pagination'
 *       404:
 *         description: Chat not found or user is not a participant
 */
router.get("/:id", ValidateNumericParams('id'), AuthenticationMiddleware, AsyncHandler(async (req, res) => {
    const data = await ChatService.findDetailsChatById(req.params?.id, req?.query, req.user?.id);
    ok(res, {message: FOUND, data})
}));

/**
 * @swagger
 * /chats/{id}/view:
 *   post:
 *     summary: Mark messages in a chat as seen
 *     tags: [Chats]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: The chat ID
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               lastSeen:
 *                 type: string
 *                 format: date-time
 *                 description: Timestamp of the last seen message
 *     responses:
 *       201:
 *         description: Successfully marked as seen
 *       403:
 *         description: Not a participant of the chat
 */
router.post("/:id/view", ValidateNumericParams('id'), AuthenticationMiddleware, AsyncHandler(async (req, res) => {
    await ChatService.seenChatMessage(req.params?.id, req.body, req.user?.id)
    created(res, {message: FOUND})
}));

module.exports = router;