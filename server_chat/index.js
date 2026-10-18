const express = require("express");
const cookieParser = require('cookie-parser');
const http = require('http');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');

require("dotenv").config();
const { logger, endStartupPhase } = require("./src/config/logger"); 

// const {CorsMiddleware} = require("./src/middleware/CorsMiddleware");
const TrimInput = require("./src/middleware/TrimInput");
const ErrorHandler = require("./src/middleware/ErrorHandler");

const SocketHandler = require("./src/sockets/socketHandlers")

const ChatController = require("./src/controller/ChatController");
const { specs, swaggerUi } = require("./src/config/swagger");

const app = express();

app.use(helmet());

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
const io = SocketHandler(server);
app.set('io', io);

const initDb = require("./src/config/initDb");

(async () => {
    try {
        await initDb();
    } catch (err) {
        logger.error("Failed to initialize database schema:", err.message);
        process.exit(1);
    }

    const port = process.env.CHAT_SERVER_PORT || process.env.PORT || 3001;
    server.listen(port, () => {
        logger.info(`Chat server is running on port ${port} (${process.env.CHAT_SERVICE_URL}).`);
        endStartupPhase();
    });
})();

// Graceful shutdown
const gracefulShutdown = (signal) => {
    logger.info(`${signal} received. Starting graceful shutdown...`);
    server.close(() => {
        logger.info('HTTP server closed.');
        const sequelize = require('./src/config/SequelizeConfig');
        const redis = require('./src/config/RedisConfig');
        Promise.all([
            sequelize.close().then(() => logger.info('Database connection closed.')),
            redis.quit().then(() => logger.info('Redis connection closed.'))
        ]).then(() => {
            logger.info('Graceful shutdown complete.');
            process.exit(0);
        }).catch((err) => {
            logger.error('Error during shutdown:', err);
            process.exit(1);
        });
    });

    setTimeout(() => {
        logger.error('Forced shutdown after timeout.');
        process.exit(1);
    }, 15000);
};

process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
process.on('SIGINT', () => gracefulShutdown('SIGINT'));