-- =============================================
-- Couple-Agent 数据库初始化脚本
-- 包含所有表结构的完整定义
-- ID 使用雪花算法在应用层生成
-- =============================================
-- 版本: 1.0.0
-- 更新: 合并 000_init_schema.sql 和 001_add_message_embedding.sql
-- =============================================

-- 1. 启用 pgvector 扩展（用于向量检索）
CREATE EXTENSION IF NOT EXISTS vector;

-- =============================================
-- 用户表 (t_user)
-- =============================================
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY,  -- 雪花 ID，由应用层生成
    user_code VARCHAR(100) NOT NULL UNIQUE,
    user_name VARCHAR(100) NOT NULL,
    user_password VARCHAR(255) NOT NULL,
    user_sex INTEGER DEFAULT 0,
    user_phone VARCHAR(20),
    address VARCHAR(500),
    max_login_num INTEGER DEFAULT 3,
    avatar VARCHAR(500),
    create_time TIMESTAMP DEFAULT NOW() NOT NULL,
    update_time TIMESTAMP DEFAULT NOW() NOT NULL,
    version INTEGER DEFAULT 0
);

-- 用户表索引
CREATE INDEX IF NOT EXISTS idx_user_code ON t_user(user_code);
CREATE INDEX IF NOT EXISTS idx_user_phone ON t_user(user_phone);

COMMENT ON TABLE t_user IS '用户表';
COMMENT ON COLUMN t_user.user_code IS '用户编码（唯一）';
COMMENT ON COLUMN t_user.user_name IS '用户名';
COMMENT ON COLUMN t_user.user_password IS '密码（加密存储）';
COMMENT ON COLUMN t_user.user_sex IS '性别: 0-未知, 1-男, 2-女';
COMMENT ON COLUMN t_user.max_login_num IS '最大同时登录数';

-- =============================================
-- 会话表 (t_conversation)
-- =============================================
CREATE TABLE IF NOT EXISTS t_conversation (
    id BIGINT PRIMARY KEY,  -- 雪花 ID，由应用层生成
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    model_code VARCHAR(50),
    last_message_id BIGINT,
    last_message_at TIMESTAMP,
    ext JSONB,
    avatar VARCHAR(512),
    current_message_id BIGINT,
    create_time TIMESTAMP DEFAULT NOW() NOT NULL,
    update_time TIMESTAMP DEFAULT NOW() NOT NULL,
    version INTEGER DEFAULT 0
);

-- 会话表索引
CREATE INDEX IF NOT EXISTS idx_conversation_user_id ON t_conversation(user_id);
CREATE INDEX IF NOT EXISTS idx_conversation_last_message_at ON t_conversation(last_message_at DESC);

COMMENT ON TABLE t_conversation IS '会话表';
COMMENT ON COLUMN t_conversation.user_id IS '所属用户 ID';
COMMENT ON COLUMN t_conversation.title IS '会话标题';
COMMENT ON COLUMN t_conversation.model_code IS '使用的模型编码';
COMMENT ON COLUMN t_conversation.ext IS '扩展信息（JSON）';

-- =============================================
-- 消息表 (t_message)
-- =============================================
CREATE TABLE IF NOT EXISTS t_message (
    id BIGINT PRIMARY KEY,  -- 雪花 ID，由应用层生成
    conversation_id BIGINT NOT NULL,
    parent_id BIGINT,
    sender_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(50) DEFAULT 'TEXT',
    token_count INTEGER DEFAULT 0,
    model_code VARCHAR(50),
    status INTEGER DEFAULT 1,
    ext JSONB,
    create_time TIMESTAMP DEFAULT NOW() NOT NULL,
    update_time TIMESTAMP DEFAULT NOW() NOT NULL,
    version INTEGER DEFAULT 0
);

-- 消息表索引
CREATE INDEX IF NOT EXISTS idx_message_conversation_id ON t_message(conversation_id);
CREATE INDEX IF NOT EXISTS idx_message_parent_id ON t_message(parent_id);
CREATE INDEX IF NOT EXISTS idx_message_sender_id ON t_message(sender_id);
CREATE INDEX IF NOT EXISTS idx_message_create_time ON t_message(create_time DESC);

COMMENT ON TABLE t_message IS '消息表';
COMMENT ON COLUMN t_message.conversation_id IS '所属会话 ID';
COMMENT ON COLUMN t_message.parent_id IS '父消息 ID（用于消息树）';
COMMENT ON COLUMN t_message.sender_id IS '发送者 ID（-1 表示 AI）';
COMMENT ON COLUMN t_message.role IS '角色: user/assistant/system';
COMMENT ON COLUMN t_message.content IS '消息内容';
COMMENT ON COLUMN t_message.content_type IS '内容类型: TEXT/IMAGE/FILE';
COMMENT ON COLUMN t_message.token_count IS 'Token 消耗数';
COMMENT ON COLUMN t_message.status IS '状态: 1-正常, 0-删除';

-- =============================================
-- 消息向量表 (t_message_embedding)
-- 用于 RAG 语义检索
-- =============================================
-- 注意: 向量维度 1536 适用于 OpenAI text-embedding-3-small
--       如使用本地模型 bge-small-zh-v1.5，维度为 512
--       建议保持 1536 以兼容多种模型
CREATE TABLE IF NOT EXISTS t_message_embedding (
    id BIGINT PRIMARY KEY,  -- 雪花 ID，由应用层生成
    message_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,  -- user/assistant
    content TEXT NOT NULL,
    embedding vector(1536),  -- 向量维度
    create_time TIMESTAMP DEFAULT NOW() NOT NULL,
    update_time TIMESTAMP DEFAULT NOW() NOT NULL,
    version INTEGER DEFAULT 0
);

-- 消息向量表索引
CREATE INDEX IF NOT EXISTS idx_msg_embed_message_id ON t_message_embedding(message_id);
CREATE INDEX IF NOT EXISTS idx_msg_embed_conversation_id ON t_message_embedding(conversation_id);
CREATE INDEX IF NOT EXISTS idx_msg_embed_user_id ON t_message_embedding(user_id);

-- HNSW 向量索引（用于高效语义检索，比 IVFFlat 更快，无需预训练）
CREATE INDEX IF NOT EXISTS idx_msg_embed_vector ON t_message_embedding 
    USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE t_message_embedding IS '消息向量存储表（RAG 语义检索）';
COMMENT ON COLUMN t_message_embedding.message_id IS '关联的消息 ID';
COMMENT ON COLUMN t_message_embedding.conversation_id IS '所属会话 ID';
COMMENT ON COLUMN t_message_embedding.user_id IS '所属用户 ID';
COMMENT ON COLUMN t_message_embedding.role IS '消息角色: user/assistant';
COMMENT ON COLUMN t_message_embedding.content IS '消息文本内容';
COMMENT ON COLUMN t_message_embedding.embedding IS '消息的向量表示（1536维）';

-- =============================================
-- 外键约束（可选，根据业务需求启用）
-- =============================================
-- 注意: 在高并发场景下，外键可能影响性能
--       建议在应用层保证数据一致性
-- 
-- ALTER TABLE t_conversation ADD CONSTRAINT fk_conversation_user 
--     FOREIGN KEY (user_id) REFERENCES t_user(id);
-- ALTER TABLE t_message ADD CONSTRAINT fk_message_conversation 
--     FOREIGN KEY (conversation_id) REFERENCES t_conversation(id);
-- ALTER TABLE t_message_embedding ADD CONSTRAINT fk_embedding_message 
--     FOREIGN KEY (message_id) REFERENCES t_message(id);

-- =============================================
-- 初始化数据（可选）
-- =============================================
-- INSERT INTO t_user (id, user_code, user_name, user_password) 
-- VALUES (1, 'admin', '管理员', '$2b$12$...');
