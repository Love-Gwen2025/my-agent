# MyAgent Python 后端

基于 FastAPI 的后端服务，使用 uv 管理依赖。与原 Java 版本并行存在，便于平滑迁移。

## 运行步骤（原生 1C2G 建议）
1. 安装 uv：参考 https://github.com/astral-sh/uv 获取二进制（无需全局虚拟环境）。
2. 在 `py-app/backend` 目录执行：
   ```bash
   uv sync
   uv run uvicorn app.main:app --host 0.0.0.0 --port 8080 --workers 1 --limit-concurrency 50
   ```
   - `--workers 1`：1 核机器避免多进程占用。
   - `--limit-concurrency 50`：限制同时请求数以控制内存。
3. 若需要容器化，可基于官方 `python:3.12-slim` 构建，启动命令同上。
4. 模型调用：在 `.env` 中填入 `AI_DEEPSEEK_API_KEY`、`AI_DEEPSEEK_BASE_URL`、`AI_DEEPSEEK_MODEL_NAME` 后，聊天接口会通过 LangChain(OpenAI 兼容) 调用 DeepSeek；未配置时聊天接口以回显占位。

## 配置
- 在 `py-app/.env` 填写数据库、Redis、JWT、模型与 OSS 等环境变量。
- 默认会从项目根的 `.env` 读取配置，可按需调整 `app/core/settings.py`。

## 状态
- 当前为骨架版，保留了主要路由与依赖注入结构，后续可逐步补齐与数据库、Redis、模型的集成逻辑。
