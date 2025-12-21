# 设计：GPT 风格分支切换

## 目标
在每条存在兄弟分支的 AI 回复下展示 `<当前/总数>` 导航，点击左右切换后，用该 checkpoint 的完整消息列表替换当前上下文，并将该 checkpoint 作为后续提问的起点。流式完成事件需返回最新 checkpointId 以便续写。

## 方案概述
- **数据流**：
  - 会话历史接口返回消息时附带 `checkpointId`，用于标记每条消息所在的状态。
  - `/checkpoint/{conversation}/siblings` 用于获取某条 AI 消息对应 checkpoint 的兄弟分支索引（current/total/siblings）。
  - `/checkpoint/{conversation}/messages` 用于按 checkpoint 拉取完整消息列表，实现整段上下文替换。
  - SSE `done` 事件增加 `checkpointId` 字段，表示生成后的最新状态，供后续发送时携带。
- **前端状态**：
  - 在 store 中增加 `currentCheckpointId`，会话切换或分支切换时更新。
  - 发送消息时把 `checkpointId` 传入请求；收到 `done` 后用返回的 `checkpointId` 更新当前分支并追加 AI 消息。
  - MessageBubble 根据 `siblingInfo` 决定是否渲染导航；点击左右时调用 siblings+messages 接口，替换 message 列表与当前分支。
- **后端实现要点**：
  - ChatService 在流式完成后读取 checkpointer 的最新 checkpoint id，并在 SSE `done` 事件中返回。
  - conversation/history 使用 CheckpointService 的 `_transform_messages`，扩展返回 `checkpointId`。
  - siblings/messages 路由保持现有结构，补充参数校验与错误处理。

## 取舍
- 不在本次支持树形可视化，仅提供 `<x/y>` 线性切换，优先完成度与交互一致性。
- 使用现有 PostgreSQL checkpointer，不引入新存储；若后续需性能优化，可另行讨论缓存方案。

## 风险与缓解
- **缺少 checkpointId 导致无法续写**：通过 SSE `done` 返回 checkpointer 最新 id，并在前端状态中持久化。
- **多分支切换顺序混乱**：按 siblings 接口返回的顺序展示，并禁用流式中的切换按钮，避免竞态。
