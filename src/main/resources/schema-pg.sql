-- ============================================
-- PostgreSQL 数据库表结构
-- 包含 pgvector 扩展支持向量搜索
-- ============================================

-- 启用 pgvector 扩展（需要数据库管理员权限）
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================
-- 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS t_user (
    id BIGSERIAL PRIMARY KEY,
    user_code VARCHAR(100) NOT NULL UNIQUE,
    user_name VARCHAR(100) NOT NULL,
    user_password VARCHAR(255) NOT NULL,
    user_sex INTEGER DEFAULT 0,
    user_phone VARCHAR(20),
    address VARCHAR(500),
    max_login_num INTEGER DEFAULT 3,
    avatar VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

COMMENT ON TABLE t_user IS '用户表';
COMMENT ON COLUMN t_user.id IS '主键ID';
COMMENT ON COLUMN t_user.user_code IS '用户编码（登录账号）';
COMMENT ON COLUMN t_user.user_name IS '用户昵称';
COMMENT ON COLUMN t_user.user_password IS '密码哈希';
COMMENT ON COLUMN t_user.user_sex IS '性别：0=男，1=女';
COMMENT ON COLUMN t_user.user_phone IS '手机号';
COMMENT ON COLUMN t_user.address IS '地址';
COMMENT ON COLUMN t_user.max_login_num IS '最大同时登录设备数';
COMMENT ON COLUMN t_user.avatar IS '头像地址';
COMMENT ON COLUMN t_user.create_time IS '创建时间';
COMMENT ON COLUMN t_user.update_time IS '更新时间';
COMMENT ON COLUMN t_user.version IS '乐观锁版本号';

-- ============================================
-- 会话表
-- ============================================
CREATE TABLE IF NOT EXISTS t_conversation (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type INTEGER NOT NULL DEFAULT 3,
    title VARCHAR(255),
    model_code VARCHAR(50),
    last_message_id BIGINT,
    last_message_at TIMESTAMP,
    ext JSONB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

COMMENT ON TABLE t_conversation IS '会话表';
COMMENT ON COLUMN t_conversation.id IS '主键ID';
COMMENT ON COLUMN t_conversation.user_id IS '所属用户ID';
COMMENT ON COLUMN t_conversation.type IS '会话类型：1=单聊，2=群聊，3=AI对话';
COMMENT ON COLUMN t_conversation.title IS '会话标题';
COMMENT ON COLUMN t_conversation.model_code IS '使用的AI模型编码';
COMMENT ON COLUMN t_conversation.last_message_id IS '最后一条消息ID';
COMMENT ON COLUMN t_conversation.last_message_at IS '最后一条消息时间';
COMMENT ON COLUMN t_conversation.ext IS '扩展信息（JSON格式）';
COMMENT ON COLUMN t_conversation.create_time IS '创建时间';
COMMENT ON COLUMN t_conversation.update_time IS '更新时间';
COMMENT ON COLUMN t_conversation.version IS '乐观锁版本号';

CREATE INDEX IF NOT EXISTS idx_conversation_user_time ON t_conversation(user_id, update_time DESC);

-- ============================================
-- 消息表
-- ============================================
CREATE TABLE IF NOT EXISTS t_message (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(50) DEFAULT 'TEXT',
    token_count INTEGER DEFAULT 0,
    model_code VARCHAR(50),
    reply_to BIGINT,
    status INTEGER DEFAULT 1,
    send_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    edit_time TIMESTAMP,
    ext JSONB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

COMMENT ON TABLE t_message IS '消息表';
COMMENT ON COLUMN t_message.id IS '主键ID';
COMMENT ON COLUMN t_message.conversation_id IS '所属会话ID';
COMMENT ON COLUMN t_message.sender_id IS '发送者ID（-1表示AI助手）';
COMMENT ON COLUMN t_message.role IS '角色：user/assistant/system';
COMMENT ON COLUMN t_message.content IS '消息内容';
COMMENT ON COLUMN t_message.content_type IS '内容类型：TEXT/IMAGE/FILE';
COMMENT ON COLUMN t_message.token_count IS 'Token数量';
COMMENT ON COLUMN t_message.model_code IS '生成消息的模型编码';
COMMENT ON COLUMN t_message.reply_to IS '回复的消息ID';
COMMENT ON COLUMN t_message.status IS '状态：1=正常，2=撤回，3=删除';
COMMENT ON COLUMN t_message.send_time IS '发送时间';
COMMENT ON COLUMN t_message.edit_time IS '编辑时间';
COMMENT ON COLUMN t_message.ext IS '扩展信息（JSON格式）';
COMMENT ON COLUMN t_message.create_time IS '创建时间';
COMMENT ON COLUMN t_message.update_time IS '更新时间';
COMMENT ON COLUMN t_message.version IS '乐观锁版本号';

CREATE INDEX IF NOT EXISTS idx_message_conv_time ON t_message(conversation_id, send_time);

-- ============================================
-- 消息向量表（用于长期记忆语义搜索）
-- 使用 all-MiniLM-L6-v2 模型，向量维度为 384
-- ============================================
CREATE TABLE IF NOT EXISTS t_message_embedding (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content_text TEXT NOT NULL,
    embedding vector(384),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE t_message_embedding IS '消息向量表（长期记忆）';
COMMENT ON COLUMN t_message_embedding.id IS '主键ID';
COMMENT ON COLUMN t_message_embedding.message_id IS '关联的消息ID';
COMMENT ON COLUMN t_message_embedding.conversation_id IS '所属会话ID';
COMMENT ON COLUMN t_message_embedding.user_id IS '所属用户ID';
COMMENT ON COLUMN t_message_embedding.content_text IS '原始文本内容';
COMMENT ON COLUMN t_message_embedding.embedding IS '向量嵌入（384维）';
COMMENT ON COLUMN t_message_embedding.create_time IS '创建时间';

-- 向量索引（使用 IVFFlat 算法，适合中等规模数据）
-- 注意：创建索引前需要有一定量的数据，建议数据量超过1000条后再创建
CREATE INDEX IF NOT EXISTS idx_embedding_conv ON t_message_embedding(conversation_id);
CREATE INDEX IF NOT EXISTS idx_embedding_user ON t_message_embedding(user_id);
-- CREATE INDEX idx_embedding_vector ON t_message_embedding USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- ============================================
-- 向量化任务表（异步处理队列）
-- ============================================
CREATE TABLE IF NOT EXISTS t_embedding_task (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content_text TEXT NOT NULL,
    status INTEGER DEFAULT 0,
    retry_count INTEGER DEFAULT 0,
    error_message TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    process_time TIMESTAMP
);

COMMENT ON TABLE t_embedding_task IS '向量化任务表';
COMMENT ON COLUMN t_embedding_task.id IS '主键ID';
COMMENT ON COLUMN t_embedding_task.message_id IS '消息ID';
COMMENT ON COLUMN t_embedding_task.conversation_id IS '会话ID';
COMMENT ON COLUMN t_embedding_task.user_id IS '用户ID';
COMMENT ON COLUMN t_embedding_task.content_text IS '待向量化的文本';
COMMENT ON COLUMN t_embedding_task.status IS '状态：0=待处理，1=处理中，2=已完成，3=失败';
COMMENT ON COLUMN t_embedding_task.retry_count IS '重试次数';
COMMENT ON COLUMN t_embedding_task.error_message IS '错误信息';
COMMENT ON COLUMN t_embedding_task.create_time IS '创建时间';
COMMENT ON COLUMN t_embedding_task.process_time IS '处理时间';

CREATE INDEX IF NOT EXISTS idx_task_status ON t_embedding_task(status, create_time);

-- ============================================
-- AI 模型配置表
-- ============================================
CREATE TABLE IF NOT EXISTS t_ai_model (
    id BIGSERIAL PRIMARY KEY,
    model_code VARCHAR(50) NOT NULL UNIQUE,
    model_name VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    api_endpoint VARCHAR(500),
    is_default BOOLEAN DEFAULT FALSE,
    is_streaming BOOLEAN DEFAULT TRUE,
    max_tokens INTEGER DEFAULT 4096,
    temperature DECIMAL(3,2) DEFAULT 0.7,
    config JSONB,
    status INTEGER DEFAULT 1,
    sort_order INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE t_ai_model IS 'AI模型配置表';
COMMENT ON COLUMN t_ai_model.id IS '主键ID';
COMMENT ON COLUMN t_ai_model.model_code IS '模型编码（唯一标识）';
COMMENT ON COLUMN t_ai_model.model_name IS '模型显示名称';
COMMENT ON COLUMN t_ai_model.provider IS '提供商：openai/azure_openai/anthropic/google_gemini';
COMMENT ON COLUMN t_ai_model.api_endpoint IS 'API端点地址';
COMMENT ON COLUMN t_ai_model.is_default IS '是否默认模型';
COMMENT ON COLUMN t_ai_model.is_streaming IS '是否支持流式输出';
COMMENT ON COLUMN t_ai_model.max_tokens IS '最大Token数';
COMMENT ON COLUMN t_ai_model.temperature IS '温度参数';
COMMENT ON COLUMN t_ai_model.config IS '其他配置（JSON格式）';
COMMENT ON COLUMN t_ai_model.status IS '状态：0=禁用，1=启用';
COMMENT ON COLUMN t_ai_model.sort_order IS '排序顺序';
COMMENT ON COLUMN t_ai_model.create_time IS '创建时间';
COMMENT ON COLUMN t_ai_model.update_time IS '更新时间';

-- ============================================
-- 初始化默认 AI 模型数据
-- ============================================
INSERT INTO t_ai_model (model_code, model_name, provider, is_default, is_streaming, max_tokens, temperature, sort_order)
VALUES
    ('gpt-4o', 'GPT-4o', 'openai', TRUE, TRUE, 4096, 0.7, 1),
    ('gpt-4o-mini', 'GPT-4o Mini', 'openai', FALSE, TRUE, 4096, 0.7, 2),
    ('gpt-3.5-turbo', 'GPT-3.5 Turbo', 'openai', FALSE, TRUE, 4096, 0.7, 3),
    ('claude-3-5-sonnet-20241022', 'Claude 3.5 Sonnet', 'anthropic', FALSE, TRUE, 4096, 0.7, 4),
    ('claude-3-opus-20240229', 'Claude 3 Opus', 'anthropic', FALSE, TRUE, 4096, 0.7, 5),
    ('gemini-1.5-pro', 'Gemini 1.5 Pro', 'google_gemini', FALSE, TRUE, 4096, 0.7, 6),
    ('gemini-1.5-flash', 'Gemini 1.5 Flash', 'google_gemini', FALSE, TRUE, 4096, 0.7, 7)
ON CONFLICT (model_code) DO NOTHING;

-- ============================================
-- 消息附件表
-- ============================================
CREATE TABLE IF NOT EXISTS t_message_attachment (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    extra JSONB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

COMMENT ON TABLE t_message_attachment IS '消息附件表';
COMMENT ON COLUMN t_message_attachment.id IS '主键ID';
COMMENT ON COLUMN t_message_attachment.message_id IS '消息ID';
COMMENT ON COLUMN t_message_attachment.file_type IS '文件类型：IMAGE/FILE/VOICE';
COMMENT ON COLUMN t_message_attachment.file_name IS '原始文件名';
COMMENT ON COLUMN t_message_attachment.file_size IS '文件大小（字节）';
COMMENT ON COLUMN t_message_attachment.url IS '文件访问地址';
COMMENT ON COLUMN t_message_attachment.extra IS '额外信息（如图片宽高等）';
COMMENT ON COLUMN t_message_attachment.create_time IS '创建时间';
COMMENT ON COLUMN t_message_attachment.update_time IS '更新时间';
COMMENT ON COLUMN t_message_attachment.version IS '乐观锁版本号';

CREATE INDEX IF NOT EXISTS idx_attachment_message ON t_message_attachment(message_id);
