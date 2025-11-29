# 手动验证清单（未执行）

1. 启动前准备：设置环境变量 USER_ACCOUNTS、DB_HOST/DB_NAME/DB_USER/DB_PASSWORD、REDIS_HOST/PORT、MONGO_URI、GPT5_ENDPOINT/GPT5_API_KEY，创建 data/uploads 目录或由应用自动创建。
2. 构建与启动：执行 `mvn clean package`，然后 `java -jar target/agent-server-1.0.0.jar`，确认控制台无错误。
3. 健康检查：访问 `GET http://localhost:3000/agent/health` 返回 `{"status":"ok"}`。
4. 登录流程：`POST /agent/user/login`（用户名密码来自 USER_ACCOUNTS），检查响应 200 且 Set-Cookie 下发；随后 `GET /agent/user/session` 返回 authenticated=true、assistantName、accounts。
5. 会话管理：`GET /agent/conversation` 返回默认会话；`POST /agent/conversation` 创建新会话；`PATCH /agent/conversation/{id}` 重命名；`DELETE /agent/conversation/{id}` 删除并验证附件文件被清理。
6. 聊天与历史：`GET /agent/conversation/history?conversationId={id}` 返回历史与附件下载地址 `/agent/uploads/**`；`POST /agent/conversation/chat` 携带消息与 1 个小文件，收到 `{reply:...}` 且新消息可在 history 中看到。
7. 登出流程：`POST /agent/user/logout` 返回成功并清理 Cookie，后续受保护接口返回 401。
