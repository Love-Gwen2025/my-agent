const fsPromises = require('fs').promises;
const path = require('path');
const { uploadDir } = require('../middleware/upload');

const deleteFiles = async (fileNames) => {
  const uniqueFiles = [...new Set(fileNames.filter(Boolean))];
  await Promise.all(
    uniqueFiles.map(async (storedName) => {
      const filePath = path.join(uploadDir, storedName);
      try {
        await fsPromises.unlink(filePath);
      } catch (error) {
        if (error.code !== 'ENOENT') {
          console.error('删除附件失败', storedName, error);
        }
      }
    })
  );
};

module.exports = {
  deleteFiles,
};
