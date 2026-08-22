const { Sequelize } = require('sequelize');
const pg = require('pg');
const { isEnvironmentProduction } = require('../utils/Utils');
require('dotenv').config();

// Load environment variables with fallback
const dbUrl = process.env.DB_URL;
const database = process.env.DB_NAME;
const user = process.env.DB_USERNAME;
const password = process.env.DB_PASSWORD;
const host = process.env.DB_HOST;
const port = process.env.DB_PORT;

const sslOption = isEnvironmentProduction()
  ? { ssl: { require: true, rejectUnauthorized: false } }
  : {};

let sequelize;
if (dbUrl && !dbUrl.startsWith('jdbc:')) {
  sequelize = new Sequelize(dbUrl, {
    dialect: 'postgres',
    dialectModule: pg,
    dialectOptions: sslOption,
    logging: false
  });
} else {
  sequelize = new Sequelize(database, user, password, {
    host: host,
    port: port,
    dialect: 'postgres',
    dialectModule: pg,
    dialectOptions: sslOption,
    logging: false
  });
}

// Test the connection
async function testConnection() {
  try {
    await sequelize.authenticate();
    console.info('Connection to the database has been established successfully.');
  } catch (error) {
    console.error('Unable to connect to the database:', error.message);
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
