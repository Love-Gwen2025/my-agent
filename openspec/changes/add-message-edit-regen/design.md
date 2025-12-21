# 设计：历史消息编辑与 AI 重新生成（分支保留）

## 目标与范围
- 在任意历史消息（默认用户消息）上提供“编辑”入口，编辑后从该 checkpoint 起生成新分支与新的 AI 回复，旧分支保留可切换。
- 在当前 AI 回复上提供“重新生成”入口，生成兄弟分支；旧回复保留。
- 依赖 `chat-branching` 能力（消息需带 checkpointId、兄弟分支查询与分支切换）。
- 不做“已编辑”标记/版本历史展示（非目标）。

## 核心流程
- **数据前提**：历史消息需携带 `checkpointId`（由 `chat-branching` 提供），SSE done 事件返回生成后的最新 `checkpointId`。
- **编辑历史消息**：
  1. 前端在用户消息上打开编辑器，提交时携带 `conversationId`、`targetMessageId`、`checkpointId`（消息所在或后置 checkpoint）、新的 `content`。
  2. 后端校验归属后，基于提供的 checkpoint 作为分叉点，使用新内容调用 Agent 生成新的回复，写入 checkpointer；返回新 AI 回复、最新 `checkpointId` 以及更新后的消息列表或可供前端重新拉取的指引。
  3. 前端用新分支的消息列表替换当前上下文，并更新当前 `checkpointId`；旧分支仍可通过 siblings + messages 切换。
- **AI 重新生成**：
  1. 前端在当前 AI 回复处提供“重新生成”按钮；调用接口时携带该回复关联的 `checkpointId`。
  2. 后端基于该 checkpoint 创建新的兄弟分支（同一用户消息上下文），生成新的 AI 回复；返回新回复与新的 `checkpointId`。
  3. 前端追加新回复，刷新 `<current/total>` 导航，更新当前 `checkpointId`。

## API 设计要点（方向性）
- 编辑接口：`POST /conversation/message/edit`（示例）入参包含 conversationId、targetMessageId、checkpointId、新 content，选项 regenerate=true；出参带新 AI 回复、最新 checkpointId（可附带消息列表）。
- 重新生成功能：在 `/chat/stream` 增加 `regenerateFromCheckpointId` 或独立 `/chat/regenerate`，输入 checkpointId；SSE done 返回新 checkpointId。
- siblings/messages 复用已有 `/checkpoint/*`，确保兼容新生成的分支。

## 状态与前端处理
- store 增加 `currentCheckpointId`，在发送/编辑/重新生成请求中传递。
- 消息列表保存每条消息的 `checkpointId`；编辑/切换/重新生成后刷新列表。
- UI 控件：
  - 用户消息：编辑按钮（历史均可）；提交后加载态，期间禁用分支导航。
  - AI 消息：当前最新回复提供“重新生成”按钮；生成时禁用导航，完成后更新 `<x/y>`。

## 权限与错误
- 所有操作需校验会话归属；编辑/重新生成失败时保留原分支不变，前端提示错误。

## 风险与缓解
- **分叉点不明确**：要求请求显式携带 checkpointId，后端以此为分叉。若找不到 checkpoint，返回错误并提示刷新历史。
- **并发编辑/生成**：在前端控制同一会话一次只允许一个编辑/生成操作；后端可加乐观校验（可选）。
