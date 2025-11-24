const path = require('path');
const express = require('express');
const session = require('express-session');
const SQLiteStore = require('connect-sqlite3')(session);

const config = require('./config');
const { ready } = require('./db/connection');
const userService = require('./services/userService');
const errorHandler = require('./middleware/errorHandler');
const { uploadDir } = require('./middleware/upload');

// Routes
const authRoutes = require('./routes/auth');
const conversationRoutes = require('./routes/conversations');
const chatRoutes = require('./routes/chat');

const app = express();
const dataDir = path.join(__dirname, '..', 'data');
const publicDir = path.join(__dirname, '..', 'public');

// Wait for database to be ready and ensure user accounts
const initPromise = (async () => {
  await ready;
  await userService.ensureAccounts();
})();

// Middleware
app.use(express.json({ limit: '1mb' }));
app.use(express.urlencoded({ extended: false }));

app.use(
  session({
    store: new SQLiteStore({
      dir: dataDir,
      db: 'sessions.db',
    }),
    secret: config.SESSION_SECRET,
    resave: false,
    saveUninitialized: false,
    cookie: {
      maxAge: config.SESSION_MAX_AGE,
      httpOnly: true,
      sameSite: 'lax',
      secure: config.NODE_ENV === 'production',
    },
  })
);

app.use(express.static(publicDir));
app.use('/uploads', express.static(uploadDir));

// Ensure initialization is complete before handling requests
app.use(async (_req, _res, next) => {
  await initPromise;
  next();
});

// Health check
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok' });
});

// API routes
app.use('/api', authRoutes);
app.use('/api/conversations', conversationRoutes);
app.use('/api', chatRoutes);

// Error handler
app.use(errorHandler);

// Serve frontend for all other routes
app.get(/^(?!\/api).*/, (req, res) => {
  res.sendFile(path.join(publicDir, 'index.html'));
});

// Start server
if (require.main === module) {
  app.listen(config.PORT, () => {
    console.log(`AI assistant server listening on http://localhost:${config.PORT}`);
  });
}

module.exports = app;
