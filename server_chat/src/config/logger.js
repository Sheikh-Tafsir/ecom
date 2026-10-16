const fs = require("fs");
const path = require("path");
const util = require("util");
const { Writable } = require("stream");
const pino = require("pino");

const LOG_DIR = process.env.LOG_DIR || path.join(process.cwd(), "logs/chat-server");

try {
    if (!fs.existsSync(LOG_DIR)) {
        fs.mkdirSync(LOG_DIR, { recursive: true });
    }
} catch (err) {
    // Ignore error if directory already exists or cannot be created immediately
}

const serverLogFile = path.join(LOG_DIR, "server.log");
const startupLogFile = path.join(LOG_DIR, "server-startup.log");

let isStartupPhase = true;
let serverFileStream = null;
let startupFileStream = null;

try {
    serverFileStream = fs.createWriteStream(serverLogFile, { flags: "a" });
    startupFileStream = fs.createWriteStream(startupLogFile, { flags: "a" });
} catch (e) {
    // Fail-safe: continue without file streams if permission error
}

const startupStreamWrapper = new Writable({
    write(chunk, encoding, callback) {
        if (isStartupPhase && startupFileStream) {
            startupFileStream.write(chunk, encoding, callback);
        } else {
            callback();
        }
    }
});

const streams = [{ stream: process.stdout }];
if (serverFileStream) {
    streams.push({ stream: serverFileStream });
}
if (startupFileStream) {
    streams.push({ stream: startupStreamWrapper });
}

const levelMap = {
    trace: 10000,
    debug: 10000,
    info: 20000,
    warn: 30000,
    error: 40000,
    fatal: 50000,
};

const baseLogger = pino(
    {
        level: process.env.LOG_LEVEL || "debug",
        timestamp: () => `,"@timestamp":"${new Date().toISOString()}"`,
        formatters: {
            level(label) {
                return {
                    level: label.toUpperCase(),
                    level_value: levelMap[label] || 20000,
                };
            },
            bindings() {
                return {
                    logger_name: "com.example.ecom.chat.ChatServer",
                    thread_name: "main",
                    "@version": "1",
                };
            },
        },
        messageKey: "message",
    },
    pino.multistream(streams)
);

function dispatchLog(level, ...args) {
    if (args.length === 0) return;
    if (
        typeof args[0] === "object" &&
        args[0] !== null &&
        !(args[0] instanceof Error) &&
        args.length === 2 &&
        typeof args[1] === "string"
    ) {
        baseLogger[level](args[0], args[1]);
    } else {
        baseLogger[level](util.format(...args));
    }
}

const logger = {
    info: (...args) => dispatchLog("info", ...args),
    warn: (...args) => dispatchLog("warn", ...args),
    error: (...args) => dispatchLog("error", ...args),
    debug: (...args) => dispatchLog("debug", ...args),
    child: (bindings) => baseLogger.child(bindings),
    pino: baseLogger,
};

// Redirect console to Pino
console.log = (...args) => logger.info(...args);
console.info = (...args) => logger.info(...args);
console.warn = (...args) => logger.warn(...args);
console.error = (...args) => logger.error(...args);
console.debug = (...args) => logger.debug(...args);

function endStartupPhase() {
    isStartupPhase = false;
    if (startupFileStream) {
        startupFileStream.end();
        startupFileStream = null;
    }
}

module.exports = {
    logger,
    endStartupPhase,
};