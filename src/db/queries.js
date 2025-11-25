const { run, get, all } = require('./connection');

// User queries
const getUserByUsername = (username) =>
  get('SELECT * FROM users WHERE username = ?', [username]);

const createUser = (username, displayName, passwordHash) =>
  run('INSERT INTO users (username, display_name, password_hash) VALUES (?, ?, ?)', [
    username,
    displayName,
    passwordHash,
  ]);

const updateUserPassword = (userId, passwordHash) =>
  run('UPDATE users SET password_hash = ? WHERE id = ?', [passwordHash, userId]);

const updateUserDisplayName = (userId, displayName) =>
  run('UPDATE users SET display_name = ? WHERE id = ?', [displayName, userId]);

// Conversation queries
const listConversations = (userId) =>
  all(
    `SELECT id, title, created_at, updated_at
     FROM conversations
     WHERE user_id = ?
     ORDER BY datetime(updated_at) DESC`,
    [userId]
  );

const getConversation = (conversationId, userId) =>
  get('SELECT * FROM conversations WHERE id = ? AND user_id = ?', [conversationId, userId]);

const createConversation = (userId, title) =>
  run('INSERT INTO conversations (user_id, title) VALUES (?, ?)', [userId, title]);

const getConversationById = (conversationId) =>
  get('SELECT id, title, created_at, updated_at FROM conversations WHERE id = ?', [
    conversationId,
  ]);

const updateConversationTitle = (conversationId, title) =>
  run('UPDATE conversations SET title = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?', [
    title,
    conversationId,
  ]);

const deleteConversation = (conversationId, userId) =>
  run('DELETE FROM conversations WHERE id = ? AND user_id = ?', [conversationId, userId]);

const touchConversation = (conversationId) =>
  run('UPDATE conversations SET updated_at = CURRENT_TIMESTAMP WHERE id = ?', [conversationId]);

// Message queries
const getMessages = (userId, conversationId) =>
  all(
    `SELECT id, role, content, created_at
     FROM messages
     WHERE user_id = ? AND conversation_id = ?
     ORDER BY id ASC`,
    [userId, conversationId]
  );

const getRecentMessages = (userId, conversationId, limit) =>
  all(
    `SELECT id, role, content
     FROM messages
     WHERE user_id = ? AND conversation_id = ?
     ORDER BY id DESC
     LIMIT ?`,
    [userId, conversationId, limit]
  );

const createMessage = (userId, conversationId, role, content) =>
  run('INSERT INTO messages (user_id, conversation_id, role, content) VALUES (?, ?, ?, ?)', [
    userId,
    conversationId,
    role,
    content,
  ]);

// Attachment queries
const getAttachmentsByMessageIds = (messageIds) => {
  if (!messageIds.length) return Promise.resolve([]);
  const placeholders = messageIds.map(() => '?').join(',');
  return all(
    `SELECT id, message_id, file_name, mime_type, stored_name, size, created_at
     FROM attachments
     WHERE message_id IN (${placeholders})
     ORDER BY id ASC`,
    messageIds
  );
};

const getAttachmentsByConversation = (conversationId) =>
  all(
    `SELECT DISTINCT attachments.stored_name
     FROM attachments
     INNER JOIN messages ON attachments.message_id = messages.id
     WHERE messages.conversation_id = ?`,
    [conversationId]
  );

const createAttachment = (messageId, fileName, mimeType, storedName, size) =>
  run(
    `INSERT INTO attachments (message_id, file_name, mime_type, stored_name, size)
     VALUES (?, ?, ?, ?, ?)`,
    [messageId, fileName, mimeType, storedName, size]
  );

module.exports = {
  // Users
  getUserByUsername,
  createUser,
  updateUserPassword,
  updateUserDisplayName,

  // Conversations
  listConversations,
  getConversation,
  createConversation,
  getConversationById,
  updateConversationTitle,
  deleteConversation,
  touchConversation,

  // Messages
  getMessages,
  getRecentMessages,
  createMessage,

  // Attachments
  getAttachmentsByMessageIds,
  getAttachmentsByConversation,
  createAttachment,
};
