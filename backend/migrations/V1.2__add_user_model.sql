-- =============================================
-- 用户自定义模型配置表
-- 版本: 1.2.0
-- =============================================

CREATE TABLE IF NOT EXISTS t_user_model (
    id BIGINT PRIMARY KEY,                    -- 雪花 ID，由应用层生成
    user_id BIGINT NOT NULL,                  -- 用户 ID
    model_name VARCHAR(100) NOT NULL,         -- 模型显示名称
    provider VARCHAR(50) NOT NULL,            -- 提供商: openai/deepseek/gemini/custom
    model_code VARCHAR(100) NOT NULL,         -- 模型编码（如 gpt-4）
    api_key VARCHAR(1000) NOT NULL,           -- API Key（Fernet 加密存储）
    base_url VARCHAR(500),                    -- Base URL（可选）
    temperature DECIMAL(3,2) DEFAULT 0.70,    -- 温度参数
    timeout INTEGER DEFAULT 30,               -- 超时(秒)
    is_default BOOLEAN DEFAULT FALSE,         -- 是否为该用户默认模型
    status INTEGER DEFAULT 1,                 -- 状态: 0=禁用 1=启用
    create_time TIMESTAMP DEFAULT NOW() NOT NULL,
    update_time TIMESTAMP DEFAULT NOW() NOT NULL,
    version INTEGER DEFAULT 0
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_user_model_user_id ON t_user_model(user_id);

-- 注释
COMMENT ON TABLE t_user_model IS '用户自定义 AI 模型配置表';
COMMENT ON COLUMN t_user_model.user_id IS '所属用户 ID';
COMMENT ON COLUMN t_user_model.model_name IS '模型显示名称';
COMMENT ON COLUMN t_user_model.provider IS '提供商: openai/deepseek/gemini/custom';
COMMENT ON COLUMN t_user_model.model_code IS '模型编码';
COMMENT ON COLUMN t_user_model.api_key IS 'API Key（加密存储）';
COMMENT ON COLUMN t_user_model.base_url IS 'Base URL';
COMMENT ON COLUMN t_user_model.temperature IS '温度参数 0-2';
COMMENT ON COLUMN t_user_model.timeout IS '请求超时秒数';
COMMENT ON COLUMN t_user_model.is_default IS '是否为用户默认模型';
COMMENT ON COLUMN t_user_model.status IS '状态: 0=禁用 1=启用';
