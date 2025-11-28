# Change: 将持久化从 SQLite3 迁移到 MySQL 并提供建表语句

## Why
- 现有 SQLite3 基于本地文件，难以支撑多实例部署、并发访问与集中备份，也缺少数据库级的权限与审计能力。
- 目标环境需要使用集中式 MySQL，便于统一运维、扩展与监控。

## What Changes
- 用 MySQL 取代 SQLite3 作为业务数据与会话数据的持久化层，提供连接池、健康检查与错误可观测性。
- 提供 InnoDB + utf8mb4 的表结构定义（users、conversations、messages、attachments，以及 session 表），包含主键、外键与必要索引。
- 补充连接配置（主机、端口、库名、账号、密码、连接池参数、时区/字符集）及启动时的校验与故障降级策略。
- 给出从现有 SQLite3 数据迁移到 MySQL 的步骤与校验点，确保迁移过程不丢数据。

## Impact
- Affected specs: database
- Affected code: src/db/connection.js, src/db/queries.js, src/server.js（session store 初始化）, src/config/index.js（新增 MySQL 配置）, 部署/环境变量文档、数据迁移脚本
