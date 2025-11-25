const multer = require('multer');

const errorHandler = (err, _req, res, _next) => {
  if (err instanceof multer.MulterError) {
    if (err.code === 'LIMIT_FILE_SIZE') {
      const config = require('../config');
      const maxMb = (config.UPLOAD_SIZE_LIMIT / (1024 * 1024)).toFixed(0);
      return res.status(400).json({ error: `单个文件大小不能超过 ${maxMb} MB` });
    }
    return res.status(400).json({ error: `上传失败：${err.message}` });
  }

  console.error('Unexpected server error', err);
  return res.status(500).json({ error: '服务器内部错误' });
};

module.exports = errorHandler;
