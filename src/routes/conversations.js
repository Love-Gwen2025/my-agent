const express = require('express');
const ensureAuth = require('../middleware/auth');
const queries = require('../db/queries');
const userService = require('../services/userService');
const conversationService = require('../services/conversationService');

const router = express.Router();

// Get all conversations
router.get('/', ensureAuth, async (req, res) => {
  try {
    let conversations = await queries.listConversations(req.session.userId);
    if (!conversations.length) {
      const createdId = await userService.ensureDefaultConversation(
        req.session.userId,
        req.session.displayName
      );
      conversations = await queries.listConversations(req.session.userId);
      req.session.activeConversationId = createdId;
    }
    return res.json({ conversations });
  } catch (error) {
    console.error('Fetch conversations error', error);
    return res.status(500).json({ error: '无法获取会话列表' });
  }
});

// Create new conversation
router.post('/', ensureAuth, async (req, res) => {
  try {
    const title = (req.body?.title || '').trim() || '新的会话';
    const result = await queries.createConversation(req.session.userId, title);
    const conversation = await queries.getConversationById(result.lastID);
    return res.status(201).json({ conversation });
  } catch (error) {
    console.error('Create conversation error', error);
    return res.status(500).json({ error: '创建会话失败' });
  }
});

// Update conversation title
router.patch('/:id', ensureAuth, async (req, res) => {
  try {
    const conversationId = Number(req.params.id);
    if (!conversationId) {
      return res.status(400).json({ error: '缺少会话 ID' });
    }
    const title = (req.body?.title || '').trim();
    if (!title) {
      return res.status(400).json({ error: '标题不能为空' });
    }
    await conversationService.ensureConversationAccess(req.session.userId, conversationId);
    await queries.updateConversationTitle(conversationId, title);
    const updated = await queries.getConversationById(conversationId);
    return res.json({ conversation: updated });
  } catch (error) {
    console.error('Rename conversation error', error);
    return res
      .status(error.statusCode || 500)
      .json({ error: error.statusCode ? error.message : '重命名失败' });
  }
});

// Delete conversation
router.delete('/:id', ensureAuth, async (req, res) => {
  try {
    const conversationId = Number(req.params.id);
    if (!conversationId) {
      return res.status(400).json({ error: '缺少会话 ID' });
    }
    await conversationService.ensureConversationAccess(req.session.userId, conversationId);
    await conversationService.deleteConversationWithFiles(conversationId, req.session.userId);
    return res.json({ message: '会话已删除' });
  } catch (error) {
    console.error('Delete conversation error', error);
    return res
      .status(error.statusCode || 500)
      .json({ error: error.statusCode ? error.message : '删除失败' });
  }
});

module.exports = router;
