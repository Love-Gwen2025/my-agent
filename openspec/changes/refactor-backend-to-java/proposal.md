# Change: 将现有 Node/Express 后端重构为 Java 技术栈（MySQL+Redis+JWT+Spring AI）

## Why
- 现有后端基于 Node/Express 与 SQLite，难以满足团队 Java 技术栈一致性、运维能力与分布式部署需求。
- 需要使用 MySQL 作为唯一数据库，Redis+JWT 实现分布式会话，满足横向扩展；同时保留现有业务接口形态。
- 引入 Spring AI 以统一 GPT 调用栈，使用 MP+MPJ 与 mapstruct 满足当前 Java 代码规范；采用 Maven 便于依赖与版本管理。

## What Changes
- 交付基于 Spring Boot 的 Java 后端，Servlet 容器统一使用 `/agent` 作为 context path，REST 接口重命名为 `/agent/user/session|login|logout`、`/agent/conversation/**`（列表/创建/改名/删除/历史/聊天），健康检查 `/agent/health`；静态资源与 `/agent/uploads/**` 保持可访问。
- 使用分层架构（controller → service → manager → DB），业务层依赖注入集中至 Base 类，manager 采用 MP+MPJ 拼接 SQL，converter 使用 mapstruct；Maven 管理依赖与构建。
- 采用 MySQL 作为业务持久化，Redis + JWT 提供分布式会话与鉴权，MongoDB 用作 AI 对话缓存介质；保留上传目录并暴露 `/agent/uploads`。
- 不保留现有数据，启动后通过配置初始化账号并创建空表/上传目录；若有历史数据需求由用户另行导入。
- 补充配置项、启动校验、错误处理与日志策略，确保与前端现有响应格式兼容（路径更新除外）。

## Impact
- Affected specs: java-backend
- Affected code: 将新增 Java 项目（Spring Boot 入口、配置、controller/service/manager/converter 等包）与 Maven 构建，替代现有 Node/Express 后端；引入 Redis 与 Spring AI 配置；数据库/会话与部署文档将同步更新，`openspec/changes/migrate-sqlite-to-mysql` 相关内容将以“新建空库/不迁移历史数据”的策略对齐或替换
