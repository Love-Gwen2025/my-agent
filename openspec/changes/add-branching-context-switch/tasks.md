## 1. 后端
- [x] 在 checkpoint 历史/消息返回结构中携带 checkpointId，确保会话历史 API 返回带 checkpoint 的消息视图。
- [x] 流式 SSE 完成事件返回最新 checkpointId，便于前端在分支续写时使用。
- [x] 校验 /checkpoint siblings/messages 接口参数与返回符合新需求，补充必要的错误处理与测试（至少健康检查或最小化接口验证）。

## 2. 前端
- [x] 状态层存储当前分支 checkpointId，并在发送消息时附带；切换分支后刷新消息列表替换上下文。
- [x] ChatPanel/MessageBubble 接入分支导航：展示 `<current/total>`，点击通过 checkpoint siblings+messages 切换分支；流式中禁用导航；切换后滚动复位。
- [x] SSE 完成事件读取 checkpointId，更新当前分支并将新消息持久化。

## 3. 验证
- [ ] 本地跑通分支切换：初始加载、切换兄弟分支、从分支继续提问后返回新分支的 `<1/2>` 状态。
- [ ] 如时间允许，补充最小化自动化验证（后端/前端）或记录手测步骤。
