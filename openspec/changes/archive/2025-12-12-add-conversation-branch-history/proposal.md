# Change: 会话历史分支化与提问版本编辑

## Why
- 当前历史查询接口仅预期返回扁平列表且实现存在未返回数据的缺口，无法标识提问的不同版本或分支，上层无法复刻 ChatGPT/Gemini “编辑提问形成分支”的体验。
- 数据模型已存在 parent_id/current_message_id 等字段但未使用，聊天上下文构建始终按线性顺序，用户无法从旧节点分叉、保留旧回答或选择活跃分支继续对话。

## What Changes
- 历史接口改为输出包含 parentId/children 的树形消息结构，并补充会话当前指针，便于前端渲染分支与定位当前版本。
- 聊天/提问接口支持基于指定节点分叉：新提问与 AI 回复沿分支保存，不覆盖旧版本，默认沿会话当前指针继续。
- 模型上下文与会话指针沿所选分支构建与更新，保证后续对话与向量记忆不混入其他分支。

## Impact
- Affected specs: conversation-history-branching
- Affected code: ConversationController/Service 历史查询、聊天入参（如 StreamChatParam）及 parentId 处理、Conversation/Message 持久化 current/parent 指针、上下文/缓存/向量任务构建逻辑
