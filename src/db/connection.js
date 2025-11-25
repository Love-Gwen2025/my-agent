const fs = require('fs');
const path = require('path');
const sqlite3 = require('sqlite3').verbose();

const dataDir = path.join(__dirname, '..', '..', 'data');
const dbPath = path.join(dataDir, 'assistant.db');

if (!fs.existsSync(dataDir)) {
  fs.mkdirSync(dataDir, { recursive: true });
}

const db = new sqlite3.Database(dbPath, (err) => {
  if (err) {
    console.error('Failed to connect to SQLite database', err);
  }
});

// Initialize database schema
db.serialize(() => {
  db.run('PRAGMA foreign_keys = ON');
  db.run(
    `CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      username TEXT NOT NULL UNIQUE,
      display_name TEXT NOT NULL,
      password_hash TEXT NOT NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )`
  );
  db.run(
    `CREATE TABLE IF NOT EXISTS conversations (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id INTEGER NOT NULL,
      title TEXT NOT NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    )`
  );
  db.run(
    `CREATE TABLE IF NOT EXISTS messages (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id INTEGER NOT NULL,
      conversation_id INTEGER NOT NULL,
      role TEXT NOT NULL CHECK(role IN ('user', 'assistant')),
      content TEXT NOT NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
      FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
    )`
  );
  db.run(
    `CREATE TABLE IF NOT EXISTS attachments (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      message_id INTEGER NOT NULL,
      file_name TEXT NOT NULL,
      mime_type TEXT NOT NULL,
      stored_name TEXT NOT NULL,
      size INTEGER NOT NULL,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
    )`
  );
});

// Helper functions to promisify database operations
const run = (sql, params = []) =>
  new Promise((resolve, reject) => {
    db.run(sql, params, function (err) {
      if (err) return reject(err);
      resolve(this);
    });
  });

const get = (sql, params = []) =>
  new Promise((resolve, reject) => {
    db.get(sql, params, (err, row) => {
      if (err) return reject(err);
      resolve(row);
    });
  });

const all = (sql, params = []) =>
  new Promise((resolve, reject) => {
    db.all(sql, params, (err, rows) => {
      if (err) return reject(err);
      resolve(rows);
    });
  });

// Migrations
const columnExists = async (table, column) => {
  const info = await all(`PRAGMA table_info(${table})`);
  return Array.isArray(info) && info.some((col) => col.name === column);
};

const ensureUsersDisplayNameColumn = async () => {
  const hasDisplayName = await columnExists('users', 'display_name');
  if (hasDisplayName) return;
  await run("ALTER TABLE users ADD COLUMN display_name TEXT DEFAULT ''");
  await run(
    `UPDATE users
     SET display_name = CASE
       WHEN TRIM(IFNULL(display_name, '')) = '' THEN username
       ELSE display_name
     END`
  );
};

const ensureMessagesConversationColumn = async () => {
  const hasConversation = await columnExists('messages', 'conversation_id');
  if (!hasConversation) {
    await run('ALTER TABLE messages ADD COLUMN conversation_id INTEGER');
  }

  const orphanMessages = await all(
    `SELECT id, user_id
     FROM messages
     WHERE conversation_id IS NULL OR conversation_id = 0`
  );
  if (!orphanMessages.length) return;

  const uniqueUserIds = [
    ...new Set(
      orphanMessages
        .map((row) => Number(row.user_id))
        .filter((id) => Number.isInteger(id) && id > 0)
    ),
  ];

  for (const userId of uniqueUserIds) {
    if (!userId) continue;
    let conversation = await get(
      'SELECT id FROM conversations WHERE user_id = ? ORDER BY id LIMIT 1',
      [userId]
    );
    if (!conversation) {
      const result = await run(
        'INSERT INTO conversations (user_id, title) VALUES (?, ?)',
        [userId, '历史导入会话']
      );
      conversation = { id: result.lastID };
    }

    await run(
      `UPDATE messages
       SET conversation_id = ?
       WHERE user_id = ? AND (conversation_id IS NULL OR conversation_id = 0)`,
      [conversation.id, userId]
    );
  }
};

const runMigrations = async () => {
  await ensureUsersDisplayNameColumn();
  await ensureMessagesConversationColumn();
};

const ready = runMigrations().catch((error) => {
  console.error('Failed to run database migrations', error);
  throw error;
});

module.exports = {
  db,
  run,
  get,
  all,
  ready,
};
