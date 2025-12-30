-- =============================================
-- 知识库模块数据库初始化脚本
-- =============================================
-- 版本: 1.0.0
-- 功能: 知识库管理、文档存储、向量检索
-- =============================================

-- =============================================
-- 知识库表 (t_knowledge_base)
-- =============================================
CREATE TABLE IF NOT EXISTS t_knowledge_base (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    document_count INTEGER DEFAULT 0,
    chunk_count INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT NOW() NOT NULL,
    update_time TIMESTAMP DEFAULT NOW() NOT NULL,
    version INTEGER DEFAULT 0
);

-- 知识库表索引
CREATE INDEX IF NOT EXISTS idx_kb_user_id ON t_knowledge_base(user_id);

COMMENT ON TABLE t_knowledge_base IS '知识库表';
COMMENT ON COLUMN t_knowledge_base.user_id IS '所属用户 ID';
COMMENT ON COLUMN t_knowledge_base.name IS '知识库名称';
COMMENT ON COLUMN t_knowledge_base.description IS '知识库描述';
COMMENT ON COLUMN t_knowledge_base.document_count IS '文档数量';
COMMENT ON COLUMN t_knowledge_base.chunk_count IS '分块总数';

-- =============================================
-- 文档表 (t_document)
-- 存储上传文档的元信息和 OSS URL
-- =============================================
CREATE TABLE IF NOT EXISTS t_document (
    id BIGINT PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(512) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(50),
    chunk_count INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'pending',
    create_time TIMESTAMP DEFAULT NOW() NOT NULL,
    update_time TIMESTAMP DEFAULT NOW() NOT NULL,
    version INTEGER DEFAULT 0
);

-- 文档表索引
CREATE INDEX IF NOT EXISTS idx_doc_kb_id ON t_document(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_doc_status ON t_document(status);

COMMENT ON TABLE t_document IS '文档表（知识库文档元信息）';
COMMENT ON COLUMN t_document.knowledge_base_id IS '所属知识库 ID';
COMMENT ON COLUMN t_document.file_name IS '原始文件名';
COMMENT ON COLUMN t_document.file_url IS 'OSS 存储 URL';
COMMENT ON COLUMN t_document.file_size IS '文件大小（字节）';
COMMENT ON COLUMN t_document.file_type IS '文件类型: pdf/docx/txt';
COMMENT ON COLUMN t_document.chunk_count IS '分块数量';
COMMENT ON COLUMN t_document.status IS '处理状态: pending/processing/done/failed';

-- =============================================
-- 文档分块表 (t_document_chunk)
-- 存储文档分块内容和向量
-- =============================================
CREATE TABLE IF NOT EXISTS t_document_chunk (
    id BIGINT PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding vector(512),
    metadata JSONB,
    create_time TIMESTAMP DEFAULT NOW() NOT NULL,
    update_time TIMESTAMP DEFAULT NOW() NOT NULL,
    version INTEGER DEFAULT 0
);

-- 文档分块表索引
CREATE INDEX IF NOT EXISTS idx_chunk_kb_id ON t_document_chunk(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_chunk_doc_id ON t_document_chunk(document_id);

-- HNSW 向量索引（用于高效语义检索）
CREATE INDEX IF NOT EXISTS idx_chunk_vector ON t_document_chunk 
    USING hnsw (embedding vector_cosine_ops);

COMMENT ON TABLE t_document_chunk IS '文档分块表（知识库向量存储）';
COMMENT ON COLUMN t_document_chunk.knowledge_base_id IS '所属知识库 ID';
COMMENT ON COLUMN t_document_chunk.document_id IS '所属文档 ID';
COMMENT ON COLUMN t_document_chunk.chunk_index IS '分块序号';
COMMENT ON COLUMN t_document_chunk.content IS '分块文本内容';
COMMENT ON COLUMN t_document_chunk.embedding IS '向量表示（512维）';
COMMENT ON COLUMN t_document_chunk.metadata IS '元数据（页码、段落等）';
