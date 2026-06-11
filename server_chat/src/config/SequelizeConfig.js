const { Sequelize } = require('sequelize');
const pg = require('pg');
require('dotenv').config();

// Load environment variables
const {
  POSTGRES_DATABASE,
  POSTGRES_USER,
  POSTGRES_PASSWORD,
  POSTGRES_HOST,
  POSTGRES_PORT,
} = process.env;


const sequelize = new Sequelize(POSTGRES_DATABASE, POSTGRES_USER, POSTGRES_PASSWORD, {
  host: POSTGRES_HOST,
  port: POSTGRES_PORT,
  dialect: 'postgres',
  dialectModule: pg,
  dialectOptions: {
    ssl: {
      require: true,
      rejectUnauthorized: false // Disable SSL validation
    }
  }
});

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
