import chalk from "chalk";

const originalLog = console.log;
const originalDebug = console.debug;
const originalInfo = console.info;
const originalWarn = console.warn;
const originalError = console.error;


console.log = (...args) => {
    originalLog(chalk.white("[LOG]"), ...args);
};

console.debug = (...args) => {
    originalInfo(chalk.green("[DEBUG]"), ...args);
};

console.info = (...args) => {
    originalInfo(chalk.blue("[INFO]"), ...args);
};

console.warn = (...args) => {
    originalWarn(chalk.yellow("[WARN]"), ...args);
};

console.error = (...args) => {
    originalError(chalk.red("[ERROR]"), ...args);
};