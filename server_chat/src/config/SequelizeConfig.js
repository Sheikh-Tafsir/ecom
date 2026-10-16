const { Sequelize } = require('sequelize');
const pg = require('pg');
const { isEnvironmentProduction } = require('../utils/Utils');
require('dotenv').config();

const { logger } = require('./logger');

// Load environment variables with fallback
const rawDbUrl = process.env.CHAT_DB_URL || process.env.DB_URL;
const dbUrl = (rawDbUrl && !rawDbUrl.startsWith('jdbc:') && rawDbUrl.trim() !== '') ? rawDbUrl : null;

const database = process.env.CHAT_DB_NAME || process.env.DB_NAME;
const user = process.env.CHAT_DB_USERNAME || process.env.DB_USERNAME;
const password = process.env.CHAT_DB_PASSWORD || process.env.DB_PASSWORD;
const host = process.env.CHAT_DB_HOST || process.env.DB_HOST;
const port = parseInt(process.env.CHAT_DB_PORT || process.env.DB_PORT, 10);

const sslOption = (isEnvironmentProduction() && process.env.DB_SSL !== 'false')
  ? { ssl: { require: true, rejectUnauthorized: false } }
  : {};

let sequelize;
const poolConfig = {
  max: parseInt(process.env.DB_POOL_MAX || '10', 10),
  min: parseInt(process.env.DB_POOL_MIN || '0', 10),
  acquire: parseInt(process.env.DB_POOL_ACQUIRE || '30000', 10),
  idle: parseInt(process.env.DB_POOL_IDLE || '10000', 10)
};

if (dbUrl) {
  sequelize = new Sequelize(dbUrl, {
    dialect: 'postgres',
    dialectModule: pg,
    dialectOptions: sslOption,
    logging: false,
    pool: poolConfig
  });
} else {
  sequelize = new Sequelize(database, user, password, {
    host: host,
    port: port,
    dialect: 'postgres',
    dialectModule: pg,
    dialectOptions: sslOption,
    logging: false,
    pool: poolConfig
  });
}

// Test the connection
async function testConnection() {
  try {
    await sequelize.authenticate();
    logger.info('Connection to the database has been established successfully.');
  } catch (error) {
    logger.error('Unable to connect to the database:', error.message);
  }
}

testConnection();

// Add global hook for trimming input strings
sequelize.addHook('beforeValidate', (instance) => {
  for (const key in instance.dataValues) {
    if (typeof instance.dataValues[key] === 'string') {
      instance.dataValues[key] = instance.dataValues[key].trim();
    }
  }
});

module.exports = sequelize;
