# Change: 前端 JWT 失效重定向修复

## Why
- 流式聊天使用 fetch 调用 `/api/chat/stream`，JWT 过期时后端返回 401 但前端仅抛出异常，未清理本地登录态也未跳转登录，用户停留在无权限页面。

## What Changes
- 在前端统一 401/JWT 失效处理策略，覆盖 axios 以及 SSE 流式接口，确保立即清理 token 与用户缓存并跳转登录。
- 调整流式聊天错误处理，使鉴权失败时能够终止流并触发统一的登录重定向。

## Impact
- Affected specs: auth-security
- Affected code: frontend/src/hooks/useSSEChat.ts, frontend/src/api/client.ts
