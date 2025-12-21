# Change: 支持历史消息编辑与 AI 重新生成分支

## Why
- 用户希望像 GPT 网页端一样，允许编辑历史消息并从该点生成新分支，同时保留旧分支便于对比。
- 需要在当前 AI 回复上直接“重新生成”，形成新的兄弟分支，而不是覆盖原回复。
- 现有分支导航提案（add-branching-context-switch）仅覆盖分支展示与切换，还缺少编辑/重新生成入口与端到端数据流。

## What Changes
- 允许编辑任意历史消息（默认针对用户消息），保存后基于目标 checkpoint 生成新分支并返回新的 AI 回复；旧分支保留，可通过导航切换。
- 为当前 AI 回复提供“重新生成”入口，生成新的兄弟分支（同一用户消息），保留原回复，done 事件返回新的 checkpointId。
- 前端在聊天 UI 中提供编辑入口和重新生成入口，切换后替换整段上下文；请求/返回统一携带 checkpointId 以支撑分支续写。
- 默认不做“已编辑/版本历史”额外标记，后续如需可再提。

## Impact
- 新增能力：`chat-edit-regenerate`（依赖已提的 `chat-branching` 分支导航能力）。
- 影响代码：后端聊天/消息/检查点路由与服务（编辑、重新生成、checkpointId 回传）；前端 ChatPanel/MessageBubble/state/SSE 事件处理与交互入口。
