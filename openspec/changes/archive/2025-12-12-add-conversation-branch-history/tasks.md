# Tasks

- [x] 梳理现状：确认 t_conversation.current_message_id 与 t_message.parent_id 在实体、Mapper、Service 中的缺口，记录 /conversation/history 未返回数据的现状。
- [x] 方案落地：定义 parentId 赋值规则（默认沿当前指针继续，分支时使用传入 parentId），补充 Conversation 等领域模型对 current 指针的持久化与更新。
- [x] 接口扩展：在流式/同步聊天入参支持 parentId，持久化新提问与 AI 回复形成分支，并更新会话当前指针。
- [x] 历史查询：基于 parentId 构建树形消息返回（含 childList 与当前指针），保证旧分支与新分支同时可见，排序与权限校验一致。
- [x] 上下文与存储：模型上下文/Redis 缓存/向量化任务沿所选分支构建，避免混入其他分支；补充必要的单元/集成用例与本地构建验证。
