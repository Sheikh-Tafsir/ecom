const cors = require('cors');
const dotenv = require('dotenv');

dotenv.config();

const allowedOrigins = (process.env.CORS_ALLOWED_ORIGINS || process.env.CLIENT_PATHS || 'http://localhost:5173,http://localhost:8080')
  .split(',')
  .map(origin => origin.trim())
  .filter(Boolean);

const corsOptions = {
  origin: (origin, callback) => {
    // Allow requests with no origin (like mobile apps, curl, server-to-server)
    if (!origin || allowedOrigins.includes(origin) || allowedOrigins.includes('*')) {
      callback(null, true);
    } else {
      callback(new Error('Not allowed by CORS'));
    }
  },
  methods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
  allowedHeaders: ["Content-Type", "Authorization", "X-XSRF-TOKEN", "Cache-Control", "Pragma", "Idempotency-Key"],
  credentials: true,
};

const CorsMiddleware = cors(corsOptions);

module.exports = {
  CorsMiddleware,
  corsOptions,
};