const express = require("express");
const cookieParser = require('cookie-parser');
const http = require('http');

require("dotenv").config();
require("./src/config/logger"); 

// const {CorsMiddleware} = require("./src/middleware/CorsMiddleware");
const TrimInput = require("./src/middleware/TrimInput");
const ErrorHandler = require("./src/middleware/ErrorHandler");

const SocketHandler = require("./src/sockets/socketHandlers")

const ChatController = require("./src/controller/ChatController");
const { isEnvironmentProduction } = require("./src/utils/Utils");
const { specs, swaggerUi } = require("./src/config/swagger");

const app = express();

app.use(
    express.json({limit: '1mb'}),
    cookieParser(),
    TrimInput
);

app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(specs));

app.get(["/", "/health"], (req, res) => {
    res.status(200).json({
        status: "UP", 
        timestamp: new Date(),
        uptime: process.uptime()
    });
});

app.use("/chats", ChatController);

app.use(ErrorHandler);

const server = http.createServer(app);
SocketHandler(server);

const listener = isEnvironmentProduction() ? app : server;

listener.listen(process.env.PORT, () => {
    console.info(`Chat server is running ${process.env.SERVER_PATH}.`);
});