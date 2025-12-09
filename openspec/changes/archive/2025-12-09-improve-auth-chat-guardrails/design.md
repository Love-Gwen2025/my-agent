# 设计说明

## 范围概述
- 鉴权安全：密码必须以 BCrypt 哈希存储与校验，退出登录需要真正清理 Redis 登录态。
- 聊天/会话健壮性：流式与同步聊天接口在执行业务前校验会话存在与成员资格，机器人会话创建接口需容错空请求体。
- 文档同步：运行指引需匹配当前 PostgreSQL + pgvector、Redis JWT、SSE/WebSocket 的实现。

## 密码哈希与兼容迁移
1. 使用已配置的 `BCryptPasswordEncoder` 对注册/修改密码进行编码后再持久化，保持与 Spring Security 生态一致。
2. 登录校验时优先认为数据库中的密码已哈希，使用 `passwordEncoder.matches(raw, encoded)`；若检测到存量值不符合 BCrypt 前缀（`$2a$/$2b$/$2y$` 等），则先执行明文等值校验，成功后立刻用 BCrypt 重新编码并回写，确保一次性迁移旧数据且不锁死存量账号。
3. 失败场景统一按现有业务码返回，避免泄露密码哈希格式或是否存在用户。

## 退出登录与 Redis 清理
1. `/user/logout` 应从 header/cookie 中获取 token（复用现有解析逻辑），调用 `ChatApiService.logout` 删除 `sessionKey` 和 `indexKey`，该方法保持幂等以便重复调用。
2. 清理后依赖现有 `JwtAuthFilter` 的 Redis 校验拒绝已注销 token，保证退出立即生效。

## 聊天与会话校验
1. 在流式与同步聊天入口校验：会话存在、当前用户属于该会话，否则返回 `CONV`/`MSG` 相关业务码且不落库、不触发模型调用。
2. 具体校验可复用 `conversationManager.selectById` 与 `existsMember`，在进入 `AiChatService` 前完成，避免无效消息写入与 Redis 记忆污染。
3. 机器人会话创建接口保持 `@RequestBody(required = false)`，当 body 为 null 或标题为空时，使用默认标题创建会话，避免 NPE。

## 文档更新
- 将运行指引与数据库章节改写为当前实际栈：PostgreSQL + pgvector 表结构、Redis 登录态、AI 模型/流式接口端点，并移除 MySQL/Mongo 旧内容，保证部署者按新架构操作。
