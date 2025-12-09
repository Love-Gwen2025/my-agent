## ADDED Requirements

### Requirement: 运行文档与现状技术栈一致
Java 后端运行指引与数据库说明 SHALL 反映当前 PostgreSQL + pgvector、Redis JWT 会话、SSE/WebSocket 接口等实现，移除 MySQL/Mongo 旧描述并提供正确的配置入口。

#### Scenario: 部署者按文档即可配置现有架构
- **WHEN** 按文档准备依赖与配置（数据库、Redis、AI 模型端点、SSE/WebSocket 路径）
- **THEN** 可与代码实际需求一致地完成部署，不会因 MySQL/Mongo 等过时信息导致启动失败或数据不匹配
