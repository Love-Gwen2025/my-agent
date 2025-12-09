# chat-conversation-safety Specification

## Purpose
TBD - created by archiving change improve-auth-chat-guardrails. Update Purpose after archive.
## Requirements
### Requirement: 聊天前校验会话存在与成员资格
系统 SHALL 在流式 `/api/chat/stream` 与同步 `/api/chat/send` 调用模型或落库消息前确认会话存在且当前用户为有效成员，否则按业务码返回并不写入消息/缓存。

#### Scenario: 非成员或不存在的会话被拒绝
- **WHEN** 用户使用不存在的会话 ID 或未加入的会话 ID 调用聊天接口
- **THEN** 系统返回对应的会话不存在/无权限错误，不会写入消息记录或触发模型调用

### Requirement: 机器人会话创建容错空请求体
系统 SHALL 在 `/conversation/create/assistant` 缺少请求体或标题为空时使用默认标题创建会话，而不是抛出空指针或 500。

#### Scenario: 无 body 也能创建默认机器人会话
- **WHEN** 客户端不传请求体或仅传空标题调用机器人会话创建
- **THEN** 系统自动补全默认标题完成创建，返回成功响应

