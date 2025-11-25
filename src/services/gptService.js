const axios = require('axios');
const config = require('../config');

const chat = async (messages) => {
  if (!config.GPT5_ENDPOINT || !config.GPT5_API_KEY) {
    throw new Error('尚未配置 GPT-5 接口信息');
  }

  const payload = {
    model: config.GPT5_MODEL,
    messages: config.SYSTEM_PROMPT
      ? [{ role: 'system', content: config.SYSTEM_PROMPT }, ...messages]
      : messages,
    temperature: config.GPT5_TEMPERATURE,
  };

  const { data } = await axios.post(config.GPT5_ENDPOINT, payload, {
    headers: {
      'Content-Type': 'application/json',
      'api-key': config.GPT5_API_KEY,
    },
    timeout: config.GPT5_TIMEOUT,
  });

  const reply = data?.choices?.[0]?.message?.content?.trim();

  if (!reply) {
    throw new Error('无效的 GPT-5 响应');
  }

  return reply;
};

const formatMessagesWithAttachments = (messages, attachmentMap) => {
  return messages.map((row) => {
    const attachments = attachmentMap.get(row.id) || [];
    let content = row.content;
    if (attachments.length) {
      const noteLines = attachments.map((att, index) => {
        const readableSize = `${(att.size / 1024).toFixed(1)} KB`;
        return `${index + 1}. 文件名：${att.file_name}，类型：${att.mime_type}，大小：${readableSize}`;
      });
      content = `${content}\n\n[附件说明]\n${noteLines.join('\n')}`;
    }
    return {
      role: row.role,
      content,
    };
  });
};

module.exports = {
  chat,
  formatMessagesWithAttachments,
};
