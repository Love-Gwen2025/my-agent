# Change: 提升鉴权与聊天流程的安全与健壮性

## Why
- 当前登录逻辑使用明文密码比对（`StringUtil.equal`），未利用已配置的 `BCryptPasswordEncoder`，存在凭证泄露与撞库风险。
- `/user/logout` 仅使 `HttpSession` 失效，未清理 Redis 登录态，旧 token 仍然可用，无法真正退出。
- 流式/同步聊天接口未校验会话归属即可落库消息，存在跨会话越权与脏数据风险；机器人会话创建接口在缺少请求体时会触发 NPE。
- 运行文档仍描述 MySQL/Mongo 旧架构，与当前 PostgreSQL + pgvector + Redis 的实现不符，易误导部署。

## What Changes
- 登录/注册/修改密码全流程改为 BCrypt 哈希存储与校验，提供对历史明文数据的兼容迁移策略。
- 退出登录时清理 Redis 会话索引与 token，确保被注销的令牌无法继续调用受保护接口。
- 聊天接口在调用大模型前校验会话存在且用户为成员，失败时按业务码返回；机器人会话创建在缺少 body 时也能安全创建默认会话。
- 更新 Java 后端运行指引与数据库说明，反映 PostgreSQL + pgvector、Redis JWT、SSE/WebSocket 接口等现状。

## Impact
- Affected specs: auth-security, chat-conversation-safety, backend-docs
- Affected code: `UserServiceImpl`, `UserController`、聊天控制器/服务、会话编排服务、运行文档（`doc/java-backend.md` 等）
