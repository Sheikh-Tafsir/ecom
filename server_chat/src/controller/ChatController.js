const express = require('express');
const AsyncHandler = require('../middleware/AsyncHandler');

const AuthenticationMiddleware = require('../middleware/AuthenticationMiddleware');
const ValidateUuidParams = require('../middleware/ValidateUuidParams');

const ChatService = require('../service/ChatService');
const BlockService = require('../service/BlockService');
const MessageService = require('../service/MessageService');
const RealtimeService = require('../service/RealtimeService');
const {FOUND, SENT} = require("../utils/Messages");
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
 *           type: string
 *           format: uuid
 *         userId:
 *           type: string
 *           format: uuid
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
 *           type: string
 *           format: uuid
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
 *           type: string
 *           format: uuid
 *         chatId:
 *           type: string
 *           format: uuid
 *         content:
 *           type: string
 *         contentType:
 *           type: string
 *           enum: [text, image, file]
 *         senderId:
 *           type: string
 *           format: uuid
 *         createdAt:
 *           type: string
 *           format: date-time
 *     Pagination:
 *       type: object
 *       properties:
 *         hasMore:
 *           type: boolean
 *         nextCursor:
 *           type: string
 *           format: uuid
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
 *           type: string
 *           format: uuid
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
    const data = await ChatService.findAllChatsByUserId(req?.query, req.user?.id, req.headers.authorization);
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
 *           type: string
 *           format: uuid
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
 *           type: string
 *           format: uuid
 *         description: Pagination cursor (id of the last message in previous page)
 *       - in: query
 *         name: afterId
 *         schema:
 *           type: string
 *           format: uuid
 *         description: Forward pagination cursor — returns messages with id greater than this value (used for fetching missed messages after reconnect)
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
router.get("/:id", ValidateUuidParams('id'), AuthenticationMiddleware, AsyncHandler(async (req, res) => {
    const data = await ChatService.findDetailsChatById(req.params?.id, req?.query, req.user?.id, req.headers.authorization);
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
 *           type: string
 *           format: uuid
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
router.post("/:id/view", ValidateUuidParams('id'), AuthenticationMiddleware, AsyncHandler(async (req, res) => {
    const chatId = req.params?.id;
    const userId = req.user?.id;
    await ChatService.seenChatMessage(chatId, req.body, userId);

    RealtimeService.notifyMessagesSeen(chatId, userId, req.body.lastSeen);

    created(res, {message: FOUND});
}));

// ==================== Send Message (HTTP) ====================

/**
 * @swagger
 * /chats/{id}/messages:
 *   post:
 *     summary: Send a message in an existing chat via HTTP
 *     tags: [Chats]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *         description: The chat ID
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - content
 *             properties:
 *               content:
 *                 type: string
 *               contentType:
 *                 type: string
 *                 enum: [text, image, file]
 *                 default: text
 *               tempId:
 *                 type: string
 *     responses:
 *       201:
 *         description: Message sent successfully
 *       400:
 *         description: Empty message or exceeds limit
 *       403:
 *         description: User is blocked or not a participant
 *       429:
 *         description: Rate limit exceeded
 */
router.post("/:id/messages", ValidateUuidParams('id'), AuthenticationMiddleware, AsyncHandler(async (req, res) => {
    const messageData = await MessageService.processAndSendMessage(
        req.user,
        { ...req.body, chatId: req.params.id }
    );
    created(res, { message: SENT, data: messageData });
}));

/**
 * @swagger
 * /chats/messages:
 *   post:
 *     summary: Send a message (existing chat or new direct conversation) via HTTP
 *     tags: [Chats]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - content
 *             properties:
 *               chatId:
 *                 type: string
 *                 format: uuid
 *               receiverId:
 *                 type: string
 *                 format: uuid
 *               content:
 *                 type: string
 *               contentType:
 *                 type: string
 *                 enum: [text, image, file]
 *                 default: text
 *               tempId:
 *                 type: string
 *     responses:
 *       201:
 *         description: Message sent successfully
 *       400:
 *         description: Empty message or invalid payload
 *       403:
 *         description: User is blocked
 *       429:
 *         description: Rate limit exceeded
 */
router.post("/messages", AuthenticationMiddleware, AsyncHandler(async (req, res) => {
    const messageData = await MessageService.processAndSendMessage(
        req.user,
        req.body
    );
    created(res, { message: SENT, data: messageData });
}));

// ==================== Block / Unblock ====================

/**
 * @swagger
 * /chats/users/{userId}/block:
 *   post:
 *     summary: Block a user
 *     tags: [Chats]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: userId
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *     responses:
 *       200:
 *         description: User blocked
 */
router.post('/users/:userId/block',
    AuthenticationMiddleware,
    ValidateUuidParams('userId'),
    AsyncHandler(async (req, res) => {
        await BlockService.blockUser(req.user.id, req.params.userId);

        // Notify both users in real-time (multi-tab/device sync)
        RealtimeService.notifyUserBlocked(req.user.id, req.params.userId, true);

        ok(res, {message: 'User blocked successfully'});
    })
);

/**
 * @swagger
 * /chats/users/{userId}/block:
 *   delete:
 *     summary: Unblock a user
 *     tags: [Chats]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: userId
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *     responses:
 *       200:
 *         description: User unblocked
 */
router.delete('/users/:userId/block',
    AuthenticationMiddleware,
    ValidateUuidParams('userId'),
    AsyncHandler(async (req, res) => {
        await BlockService.unblockUser(req.user.id, req.params.userId);

        // Notify both users in real-time (multi-tab/device sync)
        RealtimeService.notifyUserBlocked(req.user.id, req.params.userId, false);

        ok(res, {message: 'User unblocked successfully'});
    })
);

/**
 * @swagger
 * /chats/users/{userId}/block-status:
 *   get:
 *     summary: Get block status with a user
 *     tags: [Chats]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: userId
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *     responses:
 *       200:
 *         description: Block status
 */
router.get('/users/:userId/block-status',
    AuthenticationMiddleware,
    ValidateUuidParams('userId'),
    AsyncHandler(async (req, res) => {
        const status = await BlockService.getBlockStatus(req.user.id, req.params.userId);
        ok(res, {message: 'Block status retrieved', data: status});
    })
);

module.exports = router;