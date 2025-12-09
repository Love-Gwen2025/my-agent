# conversation-domain Specification

## Purpose
TBD - created by archiving change simplify-ai-conversation-model. Update Purpose after archive.
## Requirements
### Requirement: 会话限定为用户-助手单人模式
系统 SHALL 将会话定义为“单个用户与 AI 助手”的对话，不再支持多用户或群组成员关系。

#### Scenario: 会话仅绑定一个拥有者
- **WHEN** 创建或查询会话
- **THEN** 会话仅包含一个拥有者用户标识，不存在成员列表或多成员校验逻辑

### Requirement: 消息角色仅限 user/assistant
系统 SHALL 将消息发送方限定为会话拥有者或 AI 助手，`role` 仅允许 `user` 或 `assistant`。

#### Scenario: 非拥有者或第三方用户无法写入消息
- **WHEN** 请求以非会话拥有者身份发送消息
- **THEN** 系统拒绝写入并返回无权限错误，不会记录消息

### Requirement: 群聊/单聊类型字段不再驱动业务分支
系统 SHALL 移除或固定会话类型/成员相关字段，使业务逻辑不再依赖群聊/单聊类型分支。

#### Scenario: 会话类型固定为 AI 或被移除
- **WHEN** 创建/读取/校验会话
- **THEN** 不需要判断 1=单聊、2=群聊 等类型，所有会话按 AI 对话路径处理

### Requirement: 领域实体继承 BasePo 统一审计字段
系统 SHALL 让会话、消息等领域实体继承 BasePo，统一 ID、创建/更新时间和版本等审计字段，并确保表结构与映射保持一致。

#### Scenario: 实体与表结构均包含审计字段
- **WHEN** 查看会话或消息表结构与实体定义
- **THEN** 均包含 BasePo 的审计字段映射，未出现脱节或缺失

