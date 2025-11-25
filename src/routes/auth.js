const express = require('express');
const config = require('../config');
const userService = require('../services/userService');

const router = express.Router();

// Get session info
router.get('/session', async (req, res) => {
  if (req.session.userId && req.session.username) {
    return res.json({
      authenticated: true,
      user: {
        username: req.session.username,
        displayName: req.session.displayName,
      },
      assistantName: config.ASSISTANT_DISPLAY_NAME,
      accounts: config.SANITIZED_ACCOUNTS,
    });
  }
  return res.json({
    authenticated: false,
    assistantName: config.ASSISTANT_DISPLAY_NAME,
    accounts: config.SANITIZED_ACCOUNTS,
  });
});

// Login
router.post('/login', async (req, res) => {
  try {
    const username = ((req.body && req.body.username) || '').trim().toLowerCase();
    const password = (req.body && req.body.password) || '';

    if (!username || !password) {
      return res.status(400).json({ error: '请输入用户名和密码' });
    }

    const result = await userService.validateLogin(username, password);
    if (!result) {
      return res.status(401).json({ error: '账号或密码错误' });
    }

    const { user, account } = result;
    await userService.ensureDefaultConversation(user.id, account.displayName);

    req.session.userId = user.id;
    req.session.username = username;
    req.session.displayName = account.displayName;

    return res.json({ message: '登录成功' });
  } catch (error) {
    console.error('Login error', error);
    return res.status(500).json({ error: '服务器错误，登录失败' });
  }
});

// Logout
router.post('/logout', (req, res) => {
  req.session.destroy(() => {
    res.clearCookie('connect.sid');
    res.json({ message: '已退出登录' });
  });
});

module.exports = router;
