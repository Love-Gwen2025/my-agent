## 1. 调研与方案对齐
- [x] 1.1 盘点现有 Node/Express 后端的接口、数据模型、上传/GPT 调用行为，与前端依赖的响应格式逐一对齐并沉淀验收清单（含 JWT 交互方式）。
- [x] 1.2 明确与 `openspec/changes/migrate-sqlite-to-mysql` 的关系：确认不迁移历史数据，仅创建空的 MySQL 库与初始账号；记录替代说明。

## 2. 项目脚手架与基础设施
- [x] 2.1 使用 Maven 创建 Spring Boot Java 项目骨架（JDK 21），Servlet context path 配置为 `/agent`，包含 controller/service/manager/converter/domain/config/base 包结构，统一 Base 依赖注入、全局异常处理与基础日志输出（符合“无额外监控方案”的要求）。
- [x] 2.2 配置 MySQL/JDBC（连接池、超时、字符集、时区）与 Redis 单机连接（用于分布式会话），实现启动时必填项校验与 `/agent/health` 健康检查。
- [x] 2.3 集成 MP+MPJ（SQL 拼接与查询）与 mapstruct（实体转换），manager 层封装 DB 访问，service 层不直接操作持久化实体。
- [x] 2.4 集成 JWT 鉴权与 Redis 会话存储（token 黑名单/过期同步），配置 HttpOnly Cookie 透明传递方式（默认不要求 Authorization 头），确保与前端交互方式对齐。
- [x] 2.5 集成 Spring AI（GPT 调用），按官方文档配置模型、endpoint、api-key、temperature、timeout 参数（沿用现有 GPT 配置），会话上下文缓存使用 MongoDB。
- [x] 2.6 配置 MongoDB 单机连接，用于 AI 对话缓存（上下文/历史快取），定义缓存失效策略与容错逻辑。

## 3. 业务能力迁移
- [x] 3.1 建立用户/会话/消息/附件的 domain、DTO/VO、转换器，覆盖 /api/session 与登录/登出流程，使用 bcrypt 校验密码，JWT+Redis 维护登录态。
- [x] 3.2 实现会话列表/创建/重命名/删除接口 (/api/conversations)，包含鉴权与附件清理逻辑，保持现有 JSON 响应与排序。
- [x] 3.3 实现聊天与历史接口 (/api/history, /api/chat)，保留历史截断规则、Spring AI GPT 调用、消息与附件持久化与排序；上传接口沿用 /uploads 公开访问。

## 4. 数据与迁移
- [x] 4.1 设计并落地 MySQL DDL（users、conversations、messages、attachments、sessions/redis 依赖说明），满足约束与索引需求，写入仓库文档或脚本。
- [x] 4.2 定义初始化策略：根据配置创建默认账号与首个会话；说明旧数据不保留及必要的清理/重建步骤。

## 5. 验证与文档
- [x] 5.1 编写或更新集成/冒烟测试（或手动验证脚本）覆盖登录、列表、聊天、附件上传与 GPT 调用的可替代验证。
- [x] 5.2 更新运行/部署/配置文档，说明 Maven 构建、Java 启动方式、环境变量（MySQL/Redis/JWT/GPT/上传）、健康检查与“无历史数据”策略。
- [x] 5.3 运行 openspec validate 与必要的构建/测试命令，确认提案完成的交付物满足验收清单。
