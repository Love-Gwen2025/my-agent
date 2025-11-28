## Context
- 现状：业务数据使用 `sqlite3` 驱动，数据文件位于 `data/assistant.db`；会话使用 `connect-sqlite3` 将 session 存储在 `data/sessions.db`。数据表包含 users、conversations、messages、attachments，字段定义见 `src/db/connection.js`。
- 问题：SQLite 不便于多实例与集中式运维，缺少连接池/权限隔离/远程备份能力，且会话文件不利于水平扩展。
- 目标：统一切换到 MySQL，保持现有数据模型语义，新增建表语句与迁移路径，确保部署和本地开发均可使用 MySQL。

## Goals / Non-Goals
- Goals:
  - 以 MySQL（InnoDB + utf8mb4）替换 SQLite 作为业务与会话存储，支持连接池与启动期健康检查。
  - 提供完整的 MySQL 建表语句（users/conversations/messages/attachments/sessions），涵盖主键、外键、索引与默认值,书写相关注释，并备份至doc/mysql.sql文档中。
  - 补充配置项与容错逻辑（必填参数校验、错误日志、失败时的安全降级策略）。
  - 给出从现有 SQLite 数据迁移到 MySQL 的步骤与校验方法，确保数据一致。
- Non-Goals:
  - 不引入 ORM，维持轻量的 SQL 访问层。
  - 不改变业务字段/接口语义（仅后端存储实现与 DDL 迁移）。
  - 不扩展新的业务表或字段，除非为 MySQL 兼容所需的类型调整。

## Decisions
- 数据库驱动：采用 `mysql2` 的 Promise 版连接池以获得并发与池化支持；会话存储使用 `express-mysql-session` 提供的 session 表。当前环境缺少 MCP 的 context7 官方文档检索能力，因此未能在此阶段附上官方 API 文档链接，后续实现时需补查并对照官方参数配置。
- 连接配置：新增环境变量 `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PASSWORD`、`DB_CONN_LIMIT`、`DB_WAIT_FOR_CONNECTIONS`、`DB_QUEUE_LIMIT`、`DB_TIMEZONE`、`DB_CHARSET`（默认 utf8mb4）。启动时应校验必填项并在无法连接时中止对外服务。
- 架构与 DDL：
  - 通用：Engine 使用 InnoDB，字符集/排序规则使用 utf8mb4/utf8mb4_unicode_ci；时间字段使用 `TIMESTAMP`，默认 `CURRENT_TIMESTAMP`，必要处启用 `ON UPDATE CURRENT_TIMESTAMP`。
  - `users`：`id BIGINT UNSIGNED AUTO_INCREMENT` 主键，`username` 唯一索引，`display_name`、`password_hash` 必填，创建时间记录。
  - `conversations`：引用 users，保持标题与时间戳字段，`updated_at` 自动更新；索引 `(user_id, updated_at)` 以支撑会话排序。
  - `messages`：引用 users 与 conversations，`role` 使用 `ENUM('user','assistant')` 代替 SQLite 的 CHECK，内容使用 `MEDIUMTEXT` 以容纳更长文本；索引 `(conversation_id, id)` 支撑按会话顺序查询。
  - `attachments`：引用 messages，存储原文件名、MIME、存储名、尺寸，索引 `message_id`；保留 `created_at`。
  - Sessions：使用 `express-mysql-session` 默认 schema（`session_id` 主键，`expires` bigint，`data` text），并配置 TTL 与清理间隔。
- 兼容性：目标 MySQL 版本假设 ≥8.0，支持 `CHECK` 语法但为广泛兼容采用 `ENUM` 替代；若部署版本低于 8.0 需要在实现阶段确认并调整 DDL。

## Risks / Trade-offs
- 数据类型差异：SQLite 的 `DATETIME`/`TEXT` 到 MySQL 的 `TIMESTAMP`/`MEDIUMTEXT` 需要确认长度与时区，避免截断与时区漂移。
- 约束差异：SQLite 的 `CHECK` 不严格，MySQL 中用 `ENUM` 会更严格，可能暴露历史脏数据，迁移时需清洗或报错提示。
- 会话迁移：旧的 sqlite session 无法直接迁移，可能导致一次性登出，需要提前告知或提供平滑切换窗口。
- 部署依赖：引入 MySQL 后需要数据库实例、账号权限与网络连通性，开发环境需额外依赖。

## Migration Plan
- 准备：在目标 MySQL 实例创建数据库与账号，执行提供的建表语句；配置环境变量并开启最小连接池。
- 数据导入：按依赖顺序导出 SQLite（users → conversations → messages → attachments），通过脚本或批量导入写入 MySQL；检查自增主键、外键约束与行数一致性。
- 会话：可选择不迁移旧 session，统一清空并让用户重新登录；如需保留，需评估自定义迁移脚本。
- 验证：启动服务指向 MySQL，运行核心 API 冒烟与会话保持测试；如失败回滚到 SQLite 版本或保留回退配置。

## Open Questions
- 是否要求保留现有会话数据，还是可接受一次性登出？
- 目标 MySQL 版本与可用权限（是否具备创建数据库/索引/外键权限）？
- 是否需要提供自动化迁移脚本，或仅文档化手动导入步骤即可？
