# Change: 精简会话/消息实体为“用户-AI”单人对话

## Why
- 需求明确只支持用户与 AI 助手对话，不再需要人与人单聊/群聊逻辑，现有实体（会话类型、成员表、消息 senderId 等）仍保留多方能力，增加实现与维护复杂度。
- 会话/消息实体中存在类型枚举、成员关联等字段，会让后续接口/校验逻辑产生多余分支，且与实际用例不符。

## What Changes
- 将会话模型限定为“用户-助手”单人对话：移除/废弃群聊/单聊类型分支与成员表，保留唯一会话所有者。
- 消息实体收敛角色：仅保留 user/assistant 角色，不再支持第三方 senderId；限制校验与落库路径。
- 同步更新建表脚本（如 schema-pg.sql）与实体/Mapper，删除 ConversationMember 表及多类型字段；所有领域实体继承 BasePo 以携带审计字段。

## Impact
- Affected specs: conversation-domain
- Affected code: 会话/消息实体与 Mapper、Manager/Service 层校验逻辑、schema 相关定义（如 Conversation.type、ConversationMember 关联）
