const Redis = require('ioredis');
require('dotenv').config();

// Create Redis connection
const RedisConfig = new Redis(process.env.REDIS_URL);


RedisConfig.on('connect', () => console.info('✅ Redis connected'));
RedisConfig.on('ready', () => console.info('⚡ Redis ready to use'));
RedisConfig.on('reconnecting', () => console.info('🔁 Redis reconnecting...'));
RedisConfig.on('error', (err) => console.error('❌ Redis error:', err));
RedisConfig.on('end', () => console.info('🔌 Redis connection closed'));

RedisConfig.getAsync = async (key) => {
    return await RedisConfig.get(key);
};

RedisConfig.setAsync = async (key, value) => {
    return await RedisConfig.set(key, value);
};

// Close Redis connection when the application exits
process.on('exit', () => {
    RedisConfig.quit();
    console.info('Redis connection closed');
});

module.exports = RedisConfig;
