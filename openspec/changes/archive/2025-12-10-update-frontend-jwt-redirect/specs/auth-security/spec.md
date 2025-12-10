## ADDED Requirements
### Requirement: 前端 JWT 失效时统一退出并跳转
前端应用 SHALL 在检测到 JWT 失效或接口返回 401 时清理本地 token 与用户缓存，并跳转登录页，防止已失效会话停留在受保护页面。

#### Scenario: 常规接口 401 自动退出
- **WHEN** 通过 axios 发送的受保护接口返回 401
- **THEN** 前端立即移除本地 token 与用户信息，并导航到登录页

#### Scenario: 流式聊天 401 自动退出
- **WHEN** `/api/chat/stream` 请求因过期或无效 token 返回 401
- **THEN** 前端停止流式处理，清理本地 token 与用户信息，并跳转到登录页
