const express = require('express');
const path = require('path');
const ensureAuth = require('../middleware/auth');
const config = require('../config');
const queries = require('../db/queries');
const userService = require('../services/userService');
const conversationService = require('../services/conversationService');
const gptService = require('../services/gptService');
const { upload } = require('../middleware/upload');

const router = express.Router();

// Get message history
router.get('/history', ensureAuth, async (req, res) => {
  try {
    let conversationId = Number(req.query.conversationId);
    const conversations = await queries.listConversations(req.session.userId);

    if (!conversations.length) {
      const createdId = await userService.ensureDefaultConversation(
        req.session.userId,
        req.session.displayName
      );
      conversationId = createdId;
    }

    if (!conversationId) {
      conversationId = conversations[0]?.id || null;
    }

    if (!conversationId) {
      return res.json({ conversation: null, history: [] });
    }

    const conversation = await conversationService.ensureConversationAccess(
      req.session.userId,
      conversationId
    );

    const rows = await queries.getMessages(req.session.userId, conversationId);
    const messageIds = rows.map((row) => row.id);
    const attachmentRows = await queries.getAttachmentsByMessageIds(messageIds);

    const attachmentMap = attachmentRows.reduce((map, att) => {
      if (!map.has(att.message_id)) {
        map.set(att.message_id, []);
      }
      map.get(att.message_id).push({
        id: att.id,
        fileName: att.file_name,
        mimeType: att.mime_type,
        size: att.size,
        url: `/uploads/${att.stored_name}`,
        created_at: att.created_at,
      });
      return map;
    }, new Map());

    const history = rows.map((row) => ({
      role: row.role,
      content: row.content,
      created_at: row.created_at,
      attachments: attachmentMap.get(row.id) || [],
    }));

    return res.json({
      conversation: {
        id: conversation.id,
        title: conversation.title,
      },
      history,
    });
  } catch (error) {
    console.error('History error', error);
    return res.status(500).json({ error: '无法获取聊天记录' });
  }
});

// Send message and get AI response
router.post('/chat', ensureAuth, upload.array('files', config.MAX_UPLOAD_FILES), async (req, res) => {
  try {
    const rawBody = req.body || {};
    const message = (rawBody.message || '').trim();
    const conversationId = Number(rawBody.conversationId);
    const files = req.files || [];

    if (!conversationId) {
      return res.status(400).json({ error: '缺少会话 ID' });
    }

    await conversationService.ensureConversationAccess(req.session.userId, conversationId);

    if (!message && !files.length) {
      return res.status(400).json({ error: '消息或附件至少输入一个' });
    }

    // Save user message
    const insertResult = await queries.createMessage(
      req.session.userId,
      conversationId,
      'user',
      message
    );
    const userMessageId = insertResult.lastID;

    // Save attachments
    for (const file of files) {
      await queries.createAttachment(
        userMessageId,
        file.originalname,
        file.mimetype,
        path.basename(file.path),
        file.size
      );
    }

    // Get recent messages for context
    const historyRows = await queries.getRecentMessages(
      req.session.userId,
      conversationId,
      config.HISTORY_LIMIT
    );
    const messageIds = historyRows.map((row) => row.id);
    const attachmentRows = await queries.getAttachmentsByMessageIds(messageIds);

    const attachmentMap = attachmentRows.reduce((map, att) => {
      if (!map.has(att.message_id)) {
        map.set(att.message_id, []);
      }
      map.get(att.message_id).push(att);
      return map;
    }, new Map());

    const conversation = gptService.formatMessagesWithAttachments(
      historyRows.reverse(),
      attachmentMap
    );

    // Get AI response
    const reply = await gptService.chat(conversation);

    // Save assistant message
    await queries.createMessage(req.session.userId, conversationId, 'assistant', reply);
    await queries.touchConversation(conversationId);

    return res.json({ reply });
  } catch (error) {
    console.error('Chat error', error.response?.data || error.message);
    const message =
      error.response?.data?.error?.message ||
      error.message ||
      '与 GPT-5 服务通信失败，请稍后重试';
    return res.status(error.statusCode || 500).json({ error: message });
  }
});

module.exports = router;
