const ensureAuth = (req, res, next) => {
  if (!req.session.userId) {
    return res.status(401).json({ error: '未登录或会话已过期' });
  }
  return next();
};

module.exports = ensureAuth;
