const express = require("express");
const cookieParser = require('cookie-parser');
const http = require('http');

const {CorsMiddleware} = require("./src/middleware/CorsMiddleware");
const TrimInput = require("./src/middleware/TrimInput");
const ErrorHandler = require("./src/middleware/ErrorHandler");

const SocketHandler = require("./src/sockets/socketHandlers")


const ChatController = require("./src/controller/ChatController");

require("dotenv").config();
const app = express();

app.use(
    CorsMiddleware,
    express.json({limit: '5mb'}),
    cookieParser(),
    TrimInput
);

if (process.env.NODE_ENV === "production") {
    app.use(RateLimiter.speedLimiter, RateLimiter.rateLimiter);
}

app.get("/", (req, res) => {
    res.send("Hello, world!");
});

app.get("/health", (req, res) => {
    res.status(200).json({status: "UP", timestamp: new Date()});
});

app.use("/chats", ChatController);

app.use(ErrorHandler);

const server = http.createServer(app);
SocketHandler(server);

const listener = process.env.NODE_ENV == "production" ? app : server;

listener.listen(process.env.PORT, () => {
    console.info(`Server is running ${process.env.SERVER_PATH}.`);
});