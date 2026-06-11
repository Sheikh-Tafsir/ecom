const cors = require('cors');
const dotenv = require ("dotenv");

dotenv.config();

const corsOptions = {
  origin: process.env.CLIENT_PATH || "*",
  methods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
  allowedHeaders: ["Content-Type", "Authorization", 'Cache-Control', 'Pragma'],
  credentials: true,
};

const CorsMiddleware = cors(corsOptions);

module.exports = {
  CorsMiddleware,
  corsOptions,
};