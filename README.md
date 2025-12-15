# Couple-Agent Python 版本骨架

此目录用于承载 Python 后端与复用的前端代码，原 Java 项目保持不变。

## 目录
- `backend/`：FastAPI 后端代码，使用 uv 管理依赖。
- `frontend/`：从现有项目复制的前端代码，可复用原有构建流程。
- `.env`：环境变量示例，请填写数据库、Redis、JWT、模型等配置。

## 快速开始（后端）
1. 进入 `backend` 目录，安装 uv（参考 https://github.com/astral-sh/uv）。
2. 执行 `uv sync` 安装依赖。
3. 启动：`uv run uvicorn app.main:app --host 0.0.0.0 --port 8080 --workers 1 --limit-concurrency 50`
   - 针对 1 核 2G 服务器，限制 worker=1 与并发数，避免内存溢出。
4. 健康检查：访问 `GET /api/chat/health`。

## 后续迁移建议
- 按照 `openspec/changes/refactor-backend-to-python/` 中的 tasks 逐项迁移数据库、Redis、JWT、模型与 SSE/WebSocket 逻辑。
- 迁移完成后更新 `.env` 文档与前后端联调说明。 
