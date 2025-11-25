const queries = require('../db/queries');
const fileService = require('./fileService');

const ensureConversationAccess = async (userId, conversationId) => {
  const conversation = await queries.getConversation(conversationId, userId);
  if (!conversation) {
    const error = new Error('对话不存在或没有权限访问');
    error.statusCode = 404;
    throw error;
  }
  return conversation;
};

const deleteConversationWithFiles = async (conversationId, userId) => {
  const attachments = await queries.getAttachmentsByConversation(conversationId);
  const fileNames = attachments.map((item) => item.stored_name);
  await fileService.deleteFiles(fileNames);
  await queries.deleteConversation(conversationId, userId);
};

module.exports = {
  ensureConversationAccess,
  deleteConversationWithFiles,
};
