const fs = require('fs');
const path = require('path');
const multer = require('multer');
const config = require('../config');

const dataDir = path.join(__dirname, '..', '..', 'data');
const uploadDir = path.join(dataDir, 'uploads');

if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

const storage = multer.diskStorage({
  destination: uploadDir,
  filename: (_req, file, cb) => {
    const unique = `${Date.now()}-${Math.round(Math.random() * 1e9)}`;
    const ext = path.extname(file.originalname || '').slice(0, 10);
    cb(null, `${unique}${ext}`);
  },
});

const upload = multer({
  storage,
  limits: { fileSize: config.UPLOAD_SIZE_LIMIT },
});

module.exports = { upload, uploadDir };
