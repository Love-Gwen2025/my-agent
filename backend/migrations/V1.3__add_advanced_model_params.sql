-- =============================================
-- 扩展用户模型配置表 - 添加高级参数
-- 版本: 1.3.0
-- =============================================
-- 用于支持不同 AI 提供商的差异化配置参数

-- 添加 top_p 参数 (核采样)
-- OpenAI/DeepSeek/Gemini 都支持，范围 0-1
ALTER TABLE t_user_model
    ADD COLUMN IF NOT EXISTS top_p DECIMAL(3,2) DEFAULT NULL;

-- 添加 max_tokens 参数 (最大输出 token 数)
-- OpenAI/DeepSeek 支持
ALTER TABLE t_user_model
    ADD COLUMN IF NOT EXISTS max_tokens INTEGER DEFAULT NULL;

-- 添加 top_k 参数 (Gemini 专用)
-- 控制每步采样的候选 token 数量
ALTER TABLE t_user_model
    ADD COLUMN IF NOT EXISTS top_k INTEGER DEFAULT NULL;

-- 添加字段注释
COMMENT ON COLUMN t_user_model.top_p IS 'Top P 核采样参数 (0-1)，与 temperature 二选一调整';
COMMENT ON COLUMN t_user_model.max_tokens IS '最大输出 token 数';
COMMENT ON COLUMN t_user_model.top_k IS 'Top K 参数，Gemini 专用';
