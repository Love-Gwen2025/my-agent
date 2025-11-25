const bcrypt = require('bcrypt');
const config = require('../config');
const queries = require('../db/queries');

const userCache = new Map();

const ensureAccounts = async () => {
  for (const account of config.USER_ACCOUNTS) {
    const normalized = account.username.toLowerCase();
    const passwordHash = await bcrypt.hash(account.password, 12);
    const existing = await queries.getUserByUsername(normalized);

    if (!existing) {
      const result = await queries.createUser(normalized, account.displayName, passwordHash);
      userCache.set(normalized, { id: result.lastID, displayName: account.displayName });
      await ensureDefaultConversation(result.lastID, account.displayName);
    } else {
      const storedDisplayName =
        existing.display_name || existing.displayName || account.displayName || normalized;
      userCache.set(normalized, { id: existing.id, displayName: storedDisplayName });
      if (storedDisplayName !== account.displayName) {
        await queries.updateUserDisplayName(existing.id, account.displayName);
        userCache.set(normalized, { id: existing.id, displayName: account.displayName });
      }
      const matches = await bcrypt.compare(account.password, existing.password_hash);
      if (!matches) {
        await queries.updateUserPassword(existing.id, passwordHash);
      }
      await ensureDefaultConversation(existing.id, account.displayName);
    }
  }
};

const ensureDefaultConversation = async (userId, displayName) => {
  const conversations = await queries.listConversations(userId);
  const legacyTitle = `${displayName} 的第一个会话`;
  const desiredTitle = `${displayName}的第一个会话`;

  const existing = conversations[0];
  if (existing) {
    if (existing.title === legacyTitle) {
      await queries.updateConversationTitle(existing.id, desiredTitle);
    }
    return existing.id;
  }

  const result = await queries.createConversation(userId, desiredTitle);
  return result.lastID;
};

const validateLogin = async (username, password) => {
  const normalized = username.toLowerCase();
  const account = config.ACCOUNT_LOOKUP.get(normalized);
  if (!account) {
    return null;
  }

  const user = await queries.getUserByUsername(normalized);
  if (!user) {
    return null;
  }

  const isValid = await bcrypt.compare(password, user.password_hash);
  if (!isValid) {
    return null;
  }

  return { user, account };
};

module.exports = {
  userCache,
  ensureAccounts,
  ensureDefaultConversation,
  validateLogin,
};
