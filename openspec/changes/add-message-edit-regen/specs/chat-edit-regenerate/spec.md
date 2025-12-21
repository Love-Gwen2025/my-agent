## ADDED Requirements

### Requirement: 历史消息可编辑并从该点生成新分支
系统 SHALL 支持编辑任意历史消息（默认用户消息），提交后从指定 checkpoint 分叉生成新分支与新的 AI 回复，旧分支保留可切换。

#### Scenario: 编辑历史消息生成新分支
- **WHEN** 用户在会话历史中选择一条消息并提交新内容，客户端在请求中携带 `conversationId`、`targetMessageId`、`checkpointId` 和新 `content`
- **THEN** 后端校验会话归属并以该 `checkpointId` 作为分叉点生成新的回复，返回最新 `checkpointId` 以及新回复（同步或 SSE done 事件），前端用新分支的消息列表替换当前上下文，同时旧分支仍可通过分支导航切换

### Requirement: 当前 AI 回复可重新生成兄弟分支
系统 SHALL 在当前 AI 回复处提供重新生成入口，生成新的兄弟分支（同一用户消息），保留原回复并更新分支导航状态。

#### Scenario: 重新生成当前 AI 回复
- **WHEN** 用户在当前 AI 回复上点击“重新生成”，客户端携带该回复关联的 `checkpointId` 发起请求
- **THEN** 后端基于该 checkpoint 创建新的兄弟分支并生成新的 AI 回复，返回新的 `checkpointId`；前端追加该回复并刷新 `<current/total>` 导航，旧回复可继续切换

### Requirement: 分支续写携带并返回 checkpointId
系统 SHALL 在编辑/重新生成后的续写中使用当前分支的 `checkpointId` 作为起点，并在生成完成时返回最新 `checkpointId` 供后续续写。

#### Scenario: 分支续写
- **WHEN** 用户在分支切换、编辑或重新生成后继续发送新消息，客户端在请求中携带当前分支 `checkpointId`
- **THEN** 后端从该 checkpoint 恢复上下文生成回复；完成时（SSE done 或同步响应）返回新的 `checkpointId`，前端更新当前分支标识用于下一次请求

### Requirement: UI 支持编辑与重新生成入口
系统 SHALL 在聊天界面提供历史消息编辑入口和当前 AI 回复的重新生成入口，并在操作期间妥善处理加载/禁用状态。

#### Scenario: 前端交互与状态
- **WHEN** 用户展开历史消息编辑或触发重新生成
- **THEN** 前端进入加载态，禁用分支导航/重复提交；成功后用返回的分支数据刷新消息列表与 `checkpointId`，失败则提示并保持原分支不变

### Requirement: 权限校验与失败保护
系统 SHALL 校验会话归属后才允许编辑/重新生成操作；操作失败时不得破坏已有分支数据。

#### Scenario: 无权或执行失败
- **WHEN** 请求用户无权访问会话，或后端在生成过程中出错
- **THEN** 后端返回错误码/消息（如 403/500），不修改任何分支；前端提示错误并维持当前视图
