# conversation-history-branching Specification

## Purpose
TBD - created by archiving change add-conversation-branch-history. Update Purpose after archive.
## Requirements
### Requirement: 历史接口返回分支化树形消息
系统 SHALL 在会话历史查询中返回包含分支信息的树形消息结构，并提供当前活跃节点指针以支持版本化展示。

#### Scenario: 查询会话历史得到完整分支
- **WHEN** 客户端调用 GET /conversation/history?conversationId={id}
- **THEN** 响应数据包含该会话所有消息按 parentId 组装的树（根节点 parentId 为空或 0，子节点按 createTime/ID 升序），节点字段包含 id/role/content/contentType/createTime/parentId/children，且附带当前活跃消息 ID 用于标记默认分支，既有分支不会因新提问被覆盖

### Requirement: 编辑提问创建独立分支版本
系统 SHALL 将基于历史节点的编辑/重发视为新分支，保留旧提问及其回复，使用 parentId 将新版本挂载到指定基点。

#### Scenario: 指定基点重新提问
- **WHEN** 用户基于会话历史中的节点重新编辑提问并发送，请求携带 conversationId 以及 parentId（未提供则沿当前指针或最新叶子）
- **THEN** 系统创建新的用户消息并将其 parentId 设为请求中的 parentId，对应 AI 回复的 parentId 指向该用户消息，原有提问与回复记录保持不变，新旧版本在历史树中以兄弟分支共存

### Requirement: 聊天上下文按选定分支构建并更新指针
系统 SHALL 按选定分支构建模型上下文与缓存，并在生成回复后更新会话当前指针以支撑后续续写。

#### Scenario: 按分支续写与指针更新
- **WHEN** 客户端通过 /api/chat/stream 或 /api/chat/send 发送提问并传入 parentId（或未传则使用会话 currentMessageId，若也为空则取最新叶子）
- **THEN** 系统仅沿该基点到根节点的链路收集上下文与向量记忆，保存的用户与助手消息依次串联在该分支下，并在助手消息落库后将会话 currentMessageId 更新为该新助手消息 ID，后续默认续写沿同一分支执行

