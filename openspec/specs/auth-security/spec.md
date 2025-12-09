# auth-security Specification

## Purpose
TBD - created by archiving change improve-auth-chat-guardrails. Update Purpose after archive.
## Requirements
### Requirement: BCrypt 哈希存储与校验登录
系统 SHALL 在登录/注册/修改密码时使用 BCrypt 哈希，登录校验使用 `PasswordEncoder.matches`，并对存量明文数据提供一次性迁移以避免老账号被锁死。

#### Scenario: 注册或修改密码后存储哈希值
- **WHEN** 用户注册或修改密码
- **THEN** 系统使用 BCrypt 对原始密码编码后再写入用户表，数据库中不再落明文

#### Scenario: 登录兼容旧数据并回写哈希
- **WHEN** 用户登录且数据库中密码未使用 BCrypt 前缀
- **THEN** 系统先按明文比对通过后立即用 BCrypt 重算密码并回写，再返回登录成功；若密码不匹配则返回 `USER-401`

### Requirement: 退出登录必须清理 Redis 会话
系统 SHALL 在调用 `/user/logout` 时删除 Redis 中对应 token 的会话数据与索引，保证被注销的 token 立即失效。

#### Scenario: 退出后旧 token 无法再访问
- **WHEN** 用户携带 token 调用注销接口
- **THEN** 系统删除该 token 对应的 Redis session 与索引，后续携带相同 token 访问受保护接口将返回未授权

