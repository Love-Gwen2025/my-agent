# Change: 修复流式响应显示与落库延迟

## Why
- 新建对话后发送首条消息时，前端流式响应只闪现少量 token，完成后整段回答不显示，需刷新才出现，影响首轮对话体验。
- 现状推测：流式增量与完成落库之间存在状态覆盖或未写入消息列表的缺口（例如使用过期状态累加、done 事件未落消息或缺少 messageId 时清空内容）。

## What Changes
- 梳理 `useSSEChat` 与 `ChatPanel` 的流式状态管理，保证增量内容按顺序累加且不会被覆盖或闪烁。
- 在流式完成时，无论后端是否返回 messageId，都能将最终内容落入消息列表，并与后端记录对齐。
- 明确错误/终止时的 UI 行为，避免残留半截内容或强制刷新。

## Impact
- Affected specs: chat-frontend-ux (new)
- Affected code: frontend/src/hooks/useSSEChat.ts, frontend/src/components/chat/ChatPanel.tsx, 可能涉及消息存储逻辑
