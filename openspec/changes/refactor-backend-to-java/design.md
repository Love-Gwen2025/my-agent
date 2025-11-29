## Context
- 现状：后端使用 Node/Express + SQLite，路由包含健康检查、认证 (/api/session、/api/login、/api/logout)、会话管理 (/api/conversations)、聊天与历史 (/api/chat、/api/history)，使用 connect-sqlite3 维护 session，数据表为 users/conversations/messages/attachments，上传文件存储在 data/uploads 并通过 /uploads 静态暴露。
- 约束：用户要求 Java 实现，偏好 Objects.isNull 等断言式判断、依赖注入集中到 Base 类、数据库操作放在 manager 层并使用 MPJ 拼接 SQL，实体转换用 mapstruct，VO 仅在 controller 作为返回视图；需要中文注释、避免 ==。
- 新要求：数据库使用 MySQL；鉴权采用 JWT + Redis 分布式会话（JWT 透明通过 HttpOnly Cookie 传递）；ORM 层使用 MP + MPJ；AI 框架使用 Spring AI；构建使用 Maven；现有数据不保留（通过配置重建初始账号）；Servlet context path 为 /agent，会话管理接口前缀 /conversation，用户操作前缀 /user，AI 对话缓存介质使用 MongoDB。
- 变更关系：已有 `migrate-sqlite-to-mysql` 提案，本次将以“新建空库 + 不迁移旧数据”方式替代或对齐其目标；前端依赖现有 REST 路由与响应形状，应保持兼容。
- 第三方文档：当前环境未能通过 MCP context7 获取 Spring Boot/MPJ/mapstruct/Spring AI 等官方文档，实施阶段需补充官方文档核对 API 参数与配置。

## Goals / Non-Goals
- Goals:
  - 交付 Spring Boot Java 后端（Maven 构建），提供与现版等价的接口与响应，覆盖登录/会话/聊天/附件上传/GPT 调用。
  - 建立分层与基础设施：controller → service → manager（MP+MPJ SQL）、converter（mapstruct）、domain/DTO/VO，统一异常与日志，Base 类集中注入。
  - 使用 MySQL 作为唯一持久化，Redis+JWT 提供分布式会话；提供 DDL、连接池与缓存配置、健康检查；保留 /uploads 静态访问。
  - 引入 Spring AI 封装 GPT 调用，保留历史截断与 system prompt 行为。
  - 通过配置初始化账号与默认会话，不迁移既有数据。
- Non-Goals:
  - 不改动前端页面、路由路径与主要响应字段（除鉴权令牌的获取/传递方式需要对齐）。
  - 不新增新的业务功能（仅平移与必要的健壮性补充）。
  - 不在本阶段引入复杂微服务拆分或消息队列。
  - 不提供旧 SQLite 数据迁移（历史数据不保留）。

## Decisions
- 技术栈：Spring Boot 3.x + Spring MVC/Validation/Web，Maven 构建，JDK 21；密码校验沿用 bcrypt（Java 版库）；DB 访问使用 MyBatis-Plus + MPJ；对象转换使用 mapstruct；AI 调用使用 Spring AI。
- 架构分层：controller 仅做请求校验与 VO 返回；service 聚合业务流程，不直接触碰 DB；manager 封装 MP/MPJ 的 CRUD/查询；converter 包含 mapstruct 转换器；domain/DTO/VO 分离，VO 仅在 controller 输出；Base 层集中注入常用依赖。
- 配置与启动：使用 application.yml + 环境变量，覆盖端口、MySQL、Redis（单机）、MongoDB（单机，用于 AI 缓存）、JWT、GPT、上传限制等；启动前校验必填项，提供 `/agent/health` 与启动失败的明确日志；裸机部署，无额外日志/监控方案（保留基础日志输出）。
- 数据存储：MySQL（InnoDB、utf8mb4）存储 users/conversations/messages/attachments，保留外键、索引与时间戳；会话状态通过 Redis（单机）+JWT 管理（Redis 持久化 token/黑名单或会话态）；AI 对话上下文缓存使用 MongoDB（单机），定义缓存失效策略。
- GPT 集成：使用 Spring AI 对接 GPT-5 兼容端点（沿用现有 URL、鉴权、model、temperature、timeout 配置），保持 system prompt 和 HISTORY_LIMIT 逻辑，结合 MongoDB 缓存上下文或临时结果。
- 文件处理：Spring MVC Multipart 上传，存储路径沿用 data/uploads；返回 /uploads/{storedName}，删除会话时清理文件。
- 错误与日志：统一异常处理器输出 `{error:...}` JSON，记录链路日志；对上传超限、鉴权失败、外部接口异常提供明确错误信息。
- 安全与鉴权：采用 JWT + Redis 分布式会话，JWT 透明通过 HttpOnly Cookie 传递（默认不要求前端存储或 Authorization 头）；Cookie sameSite=lax、生产 secure=true；按 user_id 过滤资源访问；密码校验基于数据库存储的 hash。
- 兼容性：路由路径调整为以 `/agent` 为前缀，用户相关 `/agent/user/**`，会话管理 `/agent/conversation/**`，聊天/历史也在该前缀下；响应字段保持当前命名；支持 HISTORY_LIMIT、SYSTEM_PROMPT、ASSISTANT_DISPLAY_NAME 等配置项；上传大小与数量沿用现值，静态上传路径调整为 `/agent/uploads/**`。
- 文档与注释：代码将提供中文多段注释，尤其在 service/manager 内标注关键流程；遵守“不使用 ==、优先 Objects.isNull 等”规范。

## Risks / Trade-offs
- 兼容性风险：前端需要适配新的 context path `/agent` 与路由前缀（/user、/conversation），并确认 Cookie 传递的 JWT 行为。
- 数据清空：不保留旧数据会导致历史会话丢失，需明确沟通与告警。
- GPT 依赖：外部 GPT 服务的超时或错误处理需兼容旧行为，否则可能改变前端提示。
- 工具依赖：MP/MPJ/mapstruct/Spring AI 的用法需按官方文档配置，当前未检索到文档，实施前需补查，否则可能导致配置不当。

## Migration Plan
- 准备：搭建 MySQL 与 Redis 实例，创建上传目录；配置 Java 应用环境变量并执行 DDL。
- 数据：不迁移旧 SQLite 数据，启动后按配置自动创建初始账号与默认会话；如需导入历史数据，后续另行脚本处理。
- 切换：部署 Java 后端并指向新库/Redis，进行接口冒烟（登录、列表、聊天、上传）；确认 Cookie/Authorization 行为与会话保持正常。
- 回滚：若出现严重问题，可切回旧 Node 版本（需明确数据丢失不可逆），并在切换前备份新库数据以便后续合并。

## Open Questions
- 暂无（上述运行环境与鉴权/存储要求已确认：JWT 透明 Cookie、MySQL+Redis 单机、JDK 21、裸机部署、沿用现 GPT 配置、无额外日志/监控方案）。
