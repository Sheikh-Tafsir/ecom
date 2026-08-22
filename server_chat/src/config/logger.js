const fs = require("fs");
const path = require("path");

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

const originalLog = console.log;
const originalDebug = console.debug;
const originalInfo = console.info;
const originalWarn = console.warn;
const originalError = console.error;

function formatArgs(args) {
    return args
        .map(arg => {
            if (arg instanceof Error) {
                return arg.stack || arg.message;
            }
            if (typeof arg === "object" && arg !== null) {
                try {
                    return JSON.stringify(arg);
                } catch (e) {
                    return String(arg);
                }
            }
            return String(arg);
        })
        .join(" ");
}

function writeToFile(level, levelValue, ...args) {
    const timestamp = new Date().toISOString();
    const message = formatArgs(args);

    const logEntry = {
        "@timestamp": timestamp,
        "@version": "1",
        message: message,
        logger_name: "com.example.ecom.chat.ChatServer",
        thread_name: "main",
        level: level,
        level_value: levelValue
    };

    const line = JSON.stringify(logEntry) + "\n";

    try {
        fs.appendFileSync(serverLogFile, line);
        if (isStartupPhase) {
            fs.appendFileSync(startupLogFile, line);
        }
    } catch (e) {
        // Fail-safe: do not crash on log write failure
    }
}

console.log = (...args) => {
    originalLog("[LOG]", ...args);
    writeToFile("INFO", 20000, ...args);
};

console.debug = (...args) => {
    originalDebug("[DEBUG]", ...args);
    writeToFile("DEBUG", 10000, ...args);
};

console.info = (...args) => {
    originalInfo("[INFO]", ...args);
    writeToFile("INFO", 20000, ...args);
};

console.warn = (...args) => {
    originalWarn("[WARN]", ...args);
    writeToFile("WARN", 30000, ...args);
};

console.error = (...args) => {
    originalError("[ERROR]", ...args);
    writeToFile("ERROR", 40000, ...args);
};

function endStartupPhase() {
    isStartupPhase = false;
}

module.exports = {
    endStartupPhase,
    serverLogFile,
    startupLogFile
};