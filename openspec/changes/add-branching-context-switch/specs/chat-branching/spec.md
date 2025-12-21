## ADDED Requirements

### Requirement: 消息历史包含分支信息
系统 SHALL 在会话历史与按 checkpoint 拉取的消息列表中，为 AI/用户消息返回所属 checkpointId，以便客户端识别分支位置。

#### Scenario: 查询会话最新历史
- **WHEN** 客户端请求 `/conversation/history`（或等效历史接口）并携带会话 ID
- **THEN** 响应中的每条消息都包含 `checkpointId` 字段（字符串），用于表示该消息所在的 LangGraph 状态

#### Scenario: 按 checkpoint 拉取消息
- **WHEN** 客户端请求 `/checkpoint/{conversationId}/messages` 并携带 `checkpoint_id`
- **THEN** 响应返回该 checkpoint 对应的完整消息列表，列表内每条消息带 `checkpointId` 字段

### Requirement: 分支兄弟查询与导航
系统 SHALL 提供查询指定 checkpoint 的兄弟分支索引（current/total/siblings），用于驱动 `<1/2>` 导航显示与切换。

#### Scenario: 获取兄弟分支
- **WHEN** 客户端请求 `/checkpoint/{conversationId}/siblings` 并携带 `checkpoint_id`
- **THEN** 响应返回 `current`（0-based）、`total`、`siblings`（ID 数组），`current` 对应请求的 checkpoint 所在位置

### Requirement: 分支切换刷新上下文
系统 SHALL 在用户从 `<x/y>` 导航切换到兄弟分支时，使用目标 checkpoint 的消息列表替换当前上下文，并更新当前分支标识供后续提问使用。

#### Scenario: 点击切换分支
- **WHEN** 用户在某条有兄弟分支的 AI 消息上点击左/右切换
- **THEN** 客户端调用 siblings 确定目标 checkpoint，再调用 messages 接口获取该 checkpoint 的完整消息列表并替换当前对话视图，同时将当前分支标识更新为目标 checkpointId

### Requirement: 从选定分支继续对话
系统 SHALL 允许在切换分支后继续提问，并从该分支生成新的回复，生成完成后回传新的 checkpointId 供后续续写。

#### Scenario: 分支续写
- **WHEN** 用户在分支切换后发送新消息，客户端在请求体中携带当前 `checkpointId`
- **THEN** 后端从该 checkpoint 恢复上下文生成回复；流式 `done` 事件包含生成后的最新 `checkpointId`，前端用其更新当前分支并追加新 AI 消息

### Requirement: UI 展示分支导航
系统 SHALL 在有兄弟分支的 AI 消息下方展示 `<current/total>` 分支导航；单分支或加载中时禁用导航，切换后界面滚动保持可读。

#### Scenario: 显示与禁用逻辑
- **WHEN** 某条 AI 消息存在兄弟分支且 total>1
- **THEN** 在消息下方显示 `<current/total>` 导航并提供左右切换；在流式生成或切换请求进行中时按钮禁用；total<=1 时不显示
