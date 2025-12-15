-- =============================================
-- 消息 Embedding 表迁移脚本
-- 用于 RAG 语义检索功能
-- =============================================

-- 1. 确保 pgvector 扩展已启用
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. 创建消息 embedding 表
CREATE TABLE IF NOT EXISTS t_message_embedding (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536),  -- OpenAI text-embedding-3-small 维度
    create_time TIMESTAMP DEFAULT NOW(),
    update_time TIMESTAMP DEFAULT NOW(),
    is_deleted SMALLINT DEFAULT 0
);

-- 3. 创建索引
-- 消息 ID 索引 (外键查询)
CREATE INDEX IF NOT EXISTS idx_msg_embed_message_id ON t_message_embedding(message_id);

-- 会话 ID 索引 (按会话过滤)
CREATE INDEX IF NOT EXISTS idx_msg_embed_conversation_id ON t_message_embedding(conversation_id);

-- 用户 ID 索引 (按用户隔离)
CREATE INDEX IF NOT EXISTS idx_msg_embed_user_id ON t_message_embedding(user_id);

-- 向量索引 (HNSW - 语义检索，性能更好)
CREATE INDEX IF NOT EXISTS idx_msg_embed_vector ON t_message_embedding 
    USING hnsw (embedding vector_cosine_ops);

-- =============================================
-- 注意事项:
-- 1. 执行前确保 PostgreSQL 已安装 pgvector 扩展
-- 2. 如需更改向量维度，修改 vector(1536) 中的数字
-- 3. HNSW 索引比 IVFFlat 更快，无需预训练
-- =============================================
