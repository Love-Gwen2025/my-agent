# 设计说明

## Context
- `/conversation/history` 目前实现缺失返回值，前端只收到空数据；消息存储按创建时间线性排序，无法表达提问版本或分支。
- 数据层虽有 `parent_id`、`current_message_id` 等字段，`Conversation` 实体未映射当前指针，聊天上下文/Redis 记忆构建也未依据分支链路。
- 需求是支持类似 ChatGPT/Gemini 的“编辑提问形成分支”，既能保留旧回答，也能在指定节点继续对话。

## Goals / Non-Goals
- Goals: 输出包含分支信息的历史记录；支持基于任意节点分叉的新提问；聊天上下文/向量任务沿选定分支构建，并维护会话当前指针。
- Non-Goals: 前端 UI/交互细节（仅提供所需数据与指针）；跨会话的分支管理与回收策略；复杂的分支裁剪或配额控制。

## Decisions
1. 消息 parent 链路规则
   - 默认沿当前活跃指针继续：新用户消息的 `parentId` 取会话 `currentMessageId`（无则取最新消息或空根）。
   - 分支/编辑时：入参允许携带 `parentId` 作为分叉基点，用户消息 `parentId` 指向该节点，AI 回复 `parentId` 指向对应用户消息，旧版本与其子树保留。
   - 会话表的 `current_message_id` 字段用于持久化当前活跃叶子，便于后续默认续写。
2. 分支选择与上下文构建
   - 扩展 `/api/chat/stream`、`/api/chat/send` 入参，接受可选 `parentId`；缺省时以 `current_message_id` 或最新叶子为准。
   - 构建模型上下文/Redis 记忆时仅沿所选节点到根的链路收集消息，避免混入其他分支；生成的用户/助手消息按链路串联。
   - 在 AI 回复落库后更新 `current_message_id` 指向最新助手消息，保持默认续写分支一致。
3. 历史查询返回
   - 查询会话全部消息后按 `parentId` 组装树，根节点为 `parentId` 为空或 0 的记录，子节点按创建时间/ID 升序，节点包含 id/role/content/contentType/createTime/parentId/children。
   - 响应同时返回当前活跃指针（`currentMessageId`），以便前端标记默认分支；旧分支的节点与子树不会因新版本被覆盖。
4. 数据兼容与回填
   - `Conversation` 实体/Mapper 补充 `currentMessageId` 映射，必要时用最新消息回填；缺失 `parentId` 的历史消息在查询时按时间线性挂接，避免树结构为空洞。

## Risks / Trade-offs
- 旧数据无父子链路可能导致分支视图不完整，需要线性回填以避免空树；回填会增加查询逻辑复杂度。
- 按分支提取上下文需要额外遍历，极端长链路可能影响响应时间，初期采用简单遍历，后续再考虑缓存优化。
- 历史接口返回结构变化（树+指针）会破坏现有仅消费扁平列表的客户端，需同步前端改造与版本提示。

## Open Questions
- 是否需要限制分支深度或节点数量以控制存储与性能？当前方案暂不限制。
- 编辑同一提问是否允许复用原消息 ID？计划始终创建新记录保留旧版本，如需复用需另加删除/隐藏机制。
- 是否需要独立接口仅切换当前活跃分支而不发送消息？暂不纳入范围。

## Migration Plan
- 部署前回填会话 `current_message_id` 为各会话最新消息（若存在），必要时按时间为历史消息设置线性 `parentId`，以便树查询有链路。
- 更新接口契约文档与前端调用，确保 `/conversation/history`、`/api/chat/stream`、`/api/chat/send` 使用新的分支参数与返回格式。
