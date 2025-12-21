-- 分支管理重构：添加消息树支持
-- 执行此迁移以启用 SQL-based 分支查询

-- 1. 添加 parent_id 列（父消息 ID，用于构建分支关系）
ALTER TABLE t_message ADD COLUMN IF NOT EXISTS parent_id BIGINT REFERENCES t_message(id);

-- 2. 添加 checkpoint_id 列（关联 LangGraph checkpoint，用于恢复执行）
ALTER TABLE t_message ADD COLUMN IF NOT EXISTS checkpoint_id VARCHAR(100);

-- 3. 为 parent_id 创建索引，加速分支查询
CREATE INDEX IF NOT EXISTS idx_message_parent_id ON t_message(parent_id);

-- 4. 添加注释
COMMENT ON COLUMN t_message.parent_id IS '父消息 ID，用于构建消息树分支';
COMMENT ON COLUMN t_message.checkpoint_id IS 'LangGraph checkpoint ID，用于恢复执行';
