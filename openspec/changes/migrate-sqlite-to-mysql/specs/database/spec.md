## ADDED Requirements
### Requirement: 使用 MySQL 作为统一数据与会话存储
系统 SHALL 以 MySQL（InnoDB + utf8mb4）替换 SQLite3 作为业务数据与 session 的唯一持久化层，并在启动阶段完成连接池初始化与健康检查。

#### Scenario: MySQL 连接池就绪
- **WHEN** 服务启动且配置提供 DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD（含可选的连接池参数与时区/字符集）
- **THEN** 系统创建 MySQL 连接池并在对外提供接口前完成一次连接校验，失败时应阻止服务启动并输出可读错误日志

#### Scenario: 会话存储迁移到 MySQL
- **WHEN** express-session 初始化
- **THEN** 会话存储使用 MySQL-backed 的 session 表与过期清理策略，TTL 与 SESSION_MAX_AGE 保持一致

### Requirement: 提供 MySQL 表结构定义
系统 SHALL 提供可直接执行的 MySQL 建表语句，覆盖 users、conversations、messages、attachments、sessions 表，包含主键、外键、索引与默认值以保持现有业务约束。

```
CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(191) NOT NULL,
  display_name VARCHAR(191) NOT NULL,
  password_hash VARCHAR(191) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS conversations (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  title VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_conversations_user_updated (user_id, updated_at),
  CONSTRAINT fk_conversations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS messages (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  conversation_id BIGINT UNSIGNED NOT NULL,
  role ENUM('user','assistant') NOT NULL,
  content MEDIUMTEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_messages_conversation (conversation_id, id),
  CONSTRAINT fk_messages_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS attachments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  message_id BIGINT UNSIGNED NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  mime_type VARCHAR(255) NOT NULL,
  stored_name VARCHAR(255) NOT NULL,
  size BIGINT UNSIGNED NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_attachments_message (message_id),
  CONSTRAINT fk_attachments_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sessions (
  session_id VARCHAR(128) NOT NULL,
  expires BIGINT UNSIGNED NOT NULL,
  data MEDIUMTEXT,
  PRIMARY KEY (session_id),
  KEY idx_sessions_expires (expires)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### Scenario: 按 DDL 初始化数据库
- **WHEN** 运维在全新 MySQL 实例执行上述 SQL
- **THEN** 应成功创建全部表、主键、外键与索引，且与当前业务模型字段含义一致，无额外手工调整需求

### Requirement: 提供 SQLite 向 MySQL 的迁移路径与校验
系统 SHALL 提供从现有 SQLite 数据导入 MySQL 的步骤或脚本，并给出迁移后数据一致性的校验标准（如表行数、主键引用完整性）。

#### Scenario: 迁移验证通过
- **WHEN** 按提供的迁移方案完成导出/导入并运行校验
- **THEN** 所有表的记录数与外键约束符合预期，核心功能（登录、会话列表、消息发送、附件访问）在 MySQL 上可正常工作
