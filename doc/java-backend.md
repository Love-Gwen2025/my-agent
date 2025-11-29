# Java 后端运行指引

## 环境要求
- JDK 21
- Maven 3.9+
- MySQL（单机）
- Redis（单机，用于 JWT 会话状态）
- MongoDB（单机，用于 AI 对话缓存）

## 环境变量
- `USER_ACCOUNTS`：`username:displayName:password`，多账号用逗号分隔，必填。
- `SESSION_SECRET`：JWT 签名密钥。
- `PORT`：服务端口，默认 3000。
- `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD`：MySQL 连接配置。
- `REDIS_HOST`/`REDIS_PORT`/`REDIS_PASSWORD`/`REDIS_DATABASE`：Redis 连接配置。
- `MONGO_URI`：MongoDB 连接串，默认 `mongodb://localhost:27017/agent`。
- `GPT5_ENDPOINT`/`GPT5_API_KEY`/`GPT5_MODEL`/`GPT5_TEMPERATURE`/`GPT5_TIMEOUT_MS`：Spring AI 使用的 GPT 接口配置。
- 其他：`HISTORY_LIMIT`、`ASSISTANT_DISPLAY_NAME`、`UPLOAD_SIZE_LIMIT_MB`、`MAX_UPLOAD_FILES`、`DATA_DIR`。

## 构建与启动
1. `mvn clean package`
2. `java -jar target/agent-server-1.0.0.jar`

## 主要接口（context path = /agent）
- 健康检查：`GET /agent/health`
- 用户：`GET /agent/user/session`、`POST /agent/user/login`、`POST /agent/user/logout`
- 会话：`GET/POST /agent/conversation`、`PATCH/DELETE /agent/conversation/{id}`
- 聊天：`GET /agent/conversation/history?conversationId=...`、`POST /agent/conversation/chat`（支持表单文件上传，附件下载 `/agent/uploads/{stored_name}`）

## 数据存储
- MySQL 表定义见 `doc/mysql.sql` 或 `src/main/resources/schema.sql`，启动时自动创建。
- Redis Key：`agent:token:{token}`，存储用户 ID，TTL = `JWT_EXPIRE_MINUTES`。
- MongoDB：集合 `conversation_cache`，字段 `conversationId`、`userId`、`messages`、`updatedAt`。

## 其他说明
- 上传目录默认 `data/uploads`（可通过 `DATA_DIR` 覆盖），已映射到 `/agent/uploads/**`。
- 不保留旧数据：启动时仅按 `USER_ACCOUNTS` 创建初始账号与默认会话，如需历史迁移需自行导入。
