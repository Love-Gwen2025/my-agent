## ADDED Requirements

### Requirement: Java 后端服务框架
系统 SHALL 提供基于 Java（Spring Boot 3.x，Maven 构建）的后端应用，Servlet context path 为 `/agent`，REST 路由调整为 `/agent/user/**`（用户与登录态）、`/agent/conversation/**`（会话管理与聊天/历史），静态资源与上传路径 `/agent/uploads/**`，健康检查 `/agent/health`；采用 controller → service → manager → 数据访问分层，manager 层通过 MP+MPJ 进行 SQL 拼接查询，converter 层使用 mapstruct 进行实体与 VO 转换，Base 层集中管理依赖注入。

#### Scenario: 服务启动与健康检查
- **WHEN** 所需配置（端口、数据库、会话、GPT、MongoDB、上传目录）就绪并启动 Java 应用
- **THEN** GET `/agent/health` 返回 HTTP 200 与 `{status:"ok"}`，静态资源与 `/agent/uploads/{file}` 可被访问，代码中存在 controller/service/manager/converter/base 分层与 mapstruct 转换器的实现

### Requirement: 认证与会话管理
系统 SHALL 采用 JWT + Redis 分布式会话，支持 `/agent/user/session` 获取登录态信息、`/agent/user/login` 登录、`/agent/user/logout` 退出；密码校验使用数据库中的 hash（bcrypt 或同等强度），JWT 通过 HttpOnly Cookie 透明传递（默认不要求 Authorization 头），Cookie 需设置 HttpOnly、sameSite=lax，生产环境启用 secure，未登录或令牌失效访问受保护接口时返回 401 JSON `{error:...}`；Redis 需同步保存/失效会话信息以支持横向扩展。

#### Scenario: 登录与会话保持
- **WHEN** 用户提交有效的用户名/密码至 POST `/agent/user/login`
- **THEN** 服务校验密码，签发 JWT，写入 Redis 并通过 HttpOnly Cookie 返回；后续 GET `/agent/user/session` 携带 Cookie 返回 `authenticated:true`、用户昵称、assistantName 与 accounts 列表；POST `/agent/user/logout` 使 JWT 失效（Redis 黑名单/删除）并清理 Cookie，随后受保护接口访问被拒绝（401）

### Requirement: 会话列表与维护
系统 SHALL 提供会话列表、创建、重命名、删除接口（`/agent/conversation` GET/POST，`/agent/conversation/:id` PATCH/DELETE），仅允许访问/修改当前登录用户的数据；删除时需级联清理该会话消息及附件文件；新部署允许空数据启动并按配置自动创建默认会话。

#### Scenario: 会话增删改查
- **WHEN** 已登录用户访问 GET `/agent/conversation`
- **THEN** 返回按 `updated_at` 倒序的会话列表，若列表为空则创建默认会话并返回
- **WHEN** 用户 POST `/agent/conversation` 提供标题或空白
- **THEN** 创建会话并返回其 id/title
- **WHEN** 用户 PATCH `/agent/conversation/{id}` 提交非空标题
- **THEN** 更新标题并返回更新后的会话；若会话不存在或无权限则返回 404
- **WHEN** 用户 DELETE `/agent/conversation/{id}`
- **THEN** 删除会话、其消息与附件文件，并返回成功消息

### Requirement: 聊天历史与 GPT 交互
系统 SHALL 提供 `/agent/conversation/history` 返回指定会话的消息历史（含附件信息），以及 `/agent/conversation/chat` 接收消息与附件上传、调用 Spring AI 集成的 GPT 服务生成回复，并将用户消息与回复持久化；GPT 请求需包含可选 system prompt 与最近 HISTORY_LIMIT 条记录，失败时返回包含错误信息的 JSON。

#### Scenario: 聊天与附件上传
- **WHEN** 已登录用户调用 POST `/agent/conversation/chat`，携带 conversationId、消息文本和 ≤MAX_UPLOAD_FILES 的附件
- **THEN** 服务验证会话访问权，保存用户消息与附件元数据（包含文件名、类型、大小、存储名），构造包含附件说明的历史上下文调用 GPT，使用 MongoDB 作为对话缓存介质提升上下文读取效率，保存助手回复并更新会话的 `updated_at`，最终返回 `{reply: "..."}`
- **WHEN** 用户 GET `/agent/conversation/history` 携带 conversationId
- **THEN** 返回该会话的历史消息列表（按时间顺序）及附件下载地址 `/agent/uploads/{stored_name}`；若会话不存在返回 404

### Requirement: 数据存储与初始化
系统 SHALL 使用 MySQL（InnoDB, utf8mb4）存储 users/conversations/messages/attachments，Redis 存储会话状态，MongoDB 存储 AI 对话缓存；需提供完整 DDL/连接配置（MySQL、Redis、MongoDB），启动时若数据为空则按配置初始化账号与默认会话，附件文件沿用 data/uploads 目录并通过 `/agent/uploads` 提供；数据库/Redis/MongoDB 任一不可用时应用应拒绝对外服务并给出错误日志。

#### Scenario: 数据库与缓存可用性校验
- **WHEN** 配置的 MySQL、Redis、MongoDB 实例可用并建立连接
- **THEN** 应用启动通过健康检查；若任一不可用，启动失败且输出明确错误；首次启动在空库中自动插入配置的账号与默认会话，并能写入/读取 MongoDB 对话缓存
