# Tasks

- [x] 梳理现状：确认会话/消息相关实体、Mapper、Manager/Service 在多成员/多类型场景下的字段与校验分支，以及数据库 schema 中的依赖。
- [x] 设计收敛方案：确定 AI-only 会话的字段保留/删除清单（移除会话类型分支、删除 `ConversationMember`）、消息 sender 限制，以及必要的数据迁移/兼容策略；决定是否保留会话类型字段为固定值或彻底移除。
- [x] 更新实体与 schema：按设计调整领域实体/Mapper/枚举，剔除不再需要的字段/表（含 MessageAttachment/MessageReceipt），确保建表脚本（含 schema-pg.sql）同步；所有领域实体继承 BasePo 并保留审计字段。
- [x] 精简业务逻辑：调整 Manager/Service 层的会话创建/校验/查询/聊天逻辑，移除成员校验与群聊/单聊分支，权限以会话拥有者为唯一依据；消息仅允许 owner 与 AI 角色。
- [ ] 校验与测试：补充/更新单元或集成用例覆盖会话创建、历史查询、消息发送（仅 user/assistant 角色），验证删除成员/附件/回执表后无残留依赖，运行构建/测试确保编译通过。（已尝试 `mvn -DskipTests package`，因离线无法下载父 POM 失败，需要联网后重试）
