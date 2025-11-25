require('dotenv').config();

const parseAccounts = () => {
    const raw = (process.env.USER_ACCOUNTS || '').trim();
    if (!raw) {
        throw new Error(
            'USER_ACCOUNTS 未配置。格式：username:displayName:password，多组用逗号分隔，例如 meimei:梅梅:pass,xiaoye:小叶:pass2'
        );
    }
    return raw
        .split(',')
        .map((chunk) => chunk.trim())
        .filter(Boolean)
        .map((entry) => {
            const [username, displayName, password] = entry.split(':').map((item) => (item || '').trim());
            if (!username || !displayName || !password) {
                throw new Error(`USER_ACCOUNTS 配置错误：${entry}`);
            }
            return {username: username.toLowerCase(), displayName, password};
        });
};

const USER_ACCOUNTS = parseAccounts();

module.exports = {
    // Server
    PORT: process.env.PORT || 3000,
    NODE_ENV: process.env.NODE_ENV || 'development',

    // Session
    SESSION_SECRET: process.env.SESSION_SECRET || 'replace-me',
    SESSION_MAX_AGE: 1000 * 60 * 60 * 24 * 7, // 7 days

    // GPT-5
    GPT5_ENDPOINT: process.env.GPT5_ENDPOINT || '',
    GPT5_API_KEY: process.env.GPT5_API_KEY || '',
    GPT5_MODEL: process.env.GPT5_MODEL || 'gpt-5-chat',
    GPT5_TEMPERATURE: Number(process.env.GPT5_TEMPERATURE || 0.3),
    GPT5_TIMEOUT: Number(process.env.GPT5_TIMEOUT_MS || 30000),

    // History
    HISTORY_LIMIT: Number(process.env.HISTORY_LIMIT || 20),
    SYSTEM_PROMPT: process.env.SYSTEM_PROMPT || 'You are a helpful AI assistant that remembers each conversation.',

    // Assistant
    ASSISTANT_DISPLAY_NAME: process.env.ASSISTANT_DISPLAY_NAME || '小叶',

    // Upload
    UPLOAD_SIZE_LIMIT: Number(process.env.UPLOAD_SIZE_LIMIT_MB || 15) * 1024 * 1024, // bytes
    MAX_UPLOAD_FILES: Number(process.env.MAX_UPLOAD_FILES || 5),

    // Accounts
    USER_ACCOUNTS,
    ACCOUNT_LOOKUP: new Map(USER_ACCOUNTS.map((acc) => [acc.username, acc])),
    SANITIZED_ACCOUNTS: USER_ACCOUNTS.map(({username, displayName}) => ({
        username,
        displayName,
    })),
};
