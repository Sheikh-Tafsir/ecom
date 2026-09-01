const express = require("express");
const cookieParser = require('cookie-parser');
const http = require('http');
const rateLimit = require('express-rate-limit');

require("dotenv").config();
const { endStartupPhase } = require("./src/config/logger"); 

// const {CorsMiddleware} = require("./src/middleware/CorsMiddleware");
const TrimInput = require("./src/middleware/TrimInput");
const ErrorHandler = require("./src/middleware/ErrorHandler");

const SocketHandler = require("./src/sockets/socketHandlers")

const ChatController = require("./src/controller/ChatController");
const { specs, swaggerUi } = require("./src/config/swagger");

const app = express();

const apiLimiter = rateLimit({
    windowMs: 60 * 1000,
    max: 100,
    standardHeaders: true,
    legacyHeaders: false,
    message: { error: 'Too many requests, please try again later.' }
});

app.use(
    express.json({limit: '1mb'}),
    cookieParser(),
    TrimInput,
    apiLimiter
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

// Graceful shutdown
const gracefulShutdown = (signal) => {
    console.info(`${signal} received. Starting graceful shutdown...`);
    server.close(() => {
        console.info('HTTP server closed.');
        const sequelize = require('./src/config/SequelizeConfig');
        const redis = require('./src/config/RedisConfig');
        Promise.all([
            sequelize.close().then(() => console.info('Database connection closed.')),
            redis.quit().then(() => console.info('Redis connection closed.'))
        ]).then(() => {
            console.info('Graceful shutdown complete.');
            process.exit(0);
        }).catch((err) => {
            console.error('Error during shutdown:', err);
            process.exit(1);
        });
    });

    setTimeout(() => {
        console.error('Forced shutdown after timeout.');
        process.exit(1);
    }, 15000);
};

process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
process.on('SIGINT', () => gracefulShutdown('SIGINT'));