const Redis = require('ioredis');
require('dotenv').config();

const { logger } = require('./logger');

// Create Redis connection
const redisOptions = process.env.REDIS_URL || {
    host: process.env.REDIS_HOST,
    port: process.env.REDIS_PORT,
    password: process.env.REDIS_PASSWORD,
};

const RedisConfig = new Redis(redisOptions);

RedisConfig.on('connect', () => logger.info('✅ Redis connected'));
RedisConfig.on('ready', () => logger.info('⚡ Redis ready to use'));
RedisConfig.on('reconnecting', () => logger.info('🔁 Redis reconnecting...'));
RedisConfig.on('error', (err) => logger.error('❌ Redis error:', err));
RedisConfig.on('end', () => logger.info('🔌 Redis connection closed'));

// Close Redis connection when the application exits
process.on('exit', () => {
    try {
        if (RedisConfig.status === 'ready' || RedisConfig.status === 'connect') {
            RedisConfig.disconnect();
        }
    } catch (e) {
        // Ignore during process termination
    }
    logger.info('Redis connection closed');
});

module.exports = RedisConfig;
