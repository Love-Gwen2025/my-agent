# 接口与行为对齐清单

## 现有 Node/Express 行为
- 路由：`/api/session` 返回登录态；`/api/login`、`/api/logout`；`/api/conversations` 列表/创建；`/api/conversations/:id` PATCH/DELETE；`/api/history` 获取会话消息；`/api/chat` 发送消息与附件。
- 响应：未登录返回 `{authenticated:false, assistantName, accounts}`；登录成功后 `{authenticated:true, user:{username,displayName}, assistantName, accounts}`；错误响应形态 `{error: <中文提示>}`。
- 数据模型：users（username、display_name、password_hash）、conversations（user_id、title、created_at、updated_at）、messages（user_id、conversation_id、role、content、created_at）、attachments（message_id、file_name、mime_type、stored_name、size、created_at）；上传目录 `data/uploads` 对外暴露 `/uploads/{stored_name}`。
- 行为：会话列表按 `updated_at` 倒序；若无会话，创建默认会话（标题 `${displayName}的第一个会话`）；聊天保存用户消息与附件，按 HISTORY_LIMIT 读取近期消息并调用 GPT，保存助手回复并更新时间；会话删除级联删除附件文件。

## 新方案对齐
- 路由前缀：Servlet context `/agent`，用户相关 `/agent/user/session|login|logout`；会话相关 `/agent/conversation`（列表/创建）、`/agent/conversation/{id}`（改名/删除）、`/agent/conversation/history`、`/agent/conversation/chat`；健康检查 `/agent/health`；上传静态 `/agent/uploads/**`。
- 鉴权：登录使用 JWT 写入 HttpOnly Cookie，Redis 维护 token 状态；未登录返回 401 `{error:...}`；登录态接口返回字段与旧版一致（仅路径变化）。
- 数据：使用 MySQL 空库初始化，不迁移历史数据；启动时按 USER_ACCOUNTS 配置创建账号与默认会话；附件目录沿用 `data/uploads`。
- AI：GPT 调用沿用现有配置（endpoint/api-key/model/temperature/timeout），通过 Spring AI；历史截断规则保留；AI 对话缓存介质使用 MongoDB。

## 与迁移方案关系
- `migrate-sqlite-to-mysql` 目标由本次重构替代：直接使用 MySQL 新库（无旧数据迁移），Redis 与 MongoDB 为空白实例。
