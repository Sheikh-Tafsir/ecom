const express = require("express");
const cookieParser = require('cookie-parser');
const http = require('http');

require("dotenv").config();
const { endStartupPhase } = require("./src/config/logger"); 

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

app.get(["/", "/health"], async (req, res) => {
    const health = {
        status: "UP",
        timestamp: new Date(),
        uptime: process.uptime(),
        dependencies: {}
    };

    try {
        const sequelize = require('./src/config/SequelizeConfig');
        await sequelize.authenticate();
        health.dependencies.database = "UP";
    } catch {
        health.status = "DEGRADED";
        health.dependencies.database = "DOWN";
    }

    try {
        const redis = require('./src/config/RedisConfig');
        await redis.ping();
        health.dependencies.redis = "UP";
    } catch {
        health.status = "DEGRADED";
        health.dependencies.redis = "DOWN";
    }

    const statusCode = health.status === "UP" ? 200 : 503;
    res.status(statusCode).json(health);
});

app.get('/chats/v3/api-docs', (req, res) => {
    res.setHeader('Content-Type', 'application/json');
    res.send(specs);
});

app.use("/chats", ChatController);

app.use(ErrorHandler);

const server = http.createServer(app);
SocketHandler(server);

server.listen(process.env.CHAT_SERVER_PORT, () => {
    console.info(`Chat server is running ${process.env.CHAT_SERVICE_URL}.`);
    endStartupPhase();
});