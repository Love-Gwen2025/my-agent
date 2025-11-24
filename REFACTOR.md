# AI Assistant - 重构版本

## 项目结构

```
src/
├── config/          # 配置管理
│   └── index.js     # 统一配置入口
├── db/              # 数据库层
│   ├── connection.js  # 数据库连接和迁移
│   └── queries.js    # 数据库查询函数
├── middleware/      # 中间件
│   ├── auth.js        # 认证中间件
│   ├── errorHandler.js  # 错误处理
│   └── upload.js     # 文件上传配置
├── routes/          # 路由层
│   ├── auth.js        # 认证路由
│   ├── conversations.js  # 会话管理路由
│   └── chat.js        # 聊天路由
├── services/        # 业务逻辑层
│   ├── conversationService.js  # 会话服务
│   ├── fileService.js   # 文件服务
│   ├── gptService.js    # GPT API 服务
│   └── userService.js   # 用户服务
└── server.js        # 应用入口 (简化到 80 行)
```

## 重构改进

### 1. 模块化设计
- **配置层**: 统一管理所有环境变量
- **数据库层**: 分离连接管理和查询逻辑
- **中间件层**: 独立的认证、错误处理、上传模块
- **服务层**: 业务逻辑封装，职责清晰
- **路由层**: RESTful API 组织

### 2. 代码质量
- 消除重复代码（数据库操作统一封装）
- 单一职责原则（每个模块专注一件事）
- 依赖注入（模块间松耦合）
- 易于测试和维护

### 3. 安全性
- 移除 .env.example 中的真实密钥
- 使用占位符提醒用户配置

### 4. 可维护性
- 从 633 行单文件拆分为 14 个模块
- server.js 缩减到 80 行
- 每个文件职责明确，易于定位和修改

## 使用指南

### 1. 配置环境变量

复制 `.env.example` 为 `.env` 并填入真实配置：

```bash
cp .env.example .env
```

编辑 `.env` 文件，填入你的：
- Azure OpenAI 或 OpenAI API 配置
- Session 密钥
- 用户账号信息

### 2. 安装依赖

```bash
npm install
```

### 3. 启动服务

开发模式：
```bash
npm run dev
```

生产模式：
```bash
npm start
```

### 4. 访问应用

打开浏览器访问: http://localhost:3000

## API 端点

### 认证
- `GET /api/session` - 获取当前会话
- `POST /api/login` - 用户登录
- `POST /api/logout` - 用户登出

### 会话管理
- `GET /api/conversations` - 获取会话列表
- `POST /api/conversations` - 创建新会话
- `PATCH /api/conversations/:id` - 更新会话标题
- `DELETE /api/conversations/:id` - 删除会话

### 聊天
- `GET /api/history` - 获取历史消息
- `POST /api/chat` - 发送消息并获取 AI 回复

## 迁移说明

旧的 `server.js` 和 `db.js` 已备份为：
- `src/server.js.old`
- `src/db.js.old`

新版本完全兼容原有数据库和 API，无需迁移数据。

## 后续优化建议

1. 添加单元测试和集成测试
2. 添加 API 参数验证库（如 joi 或 zod）
3. 添加日志系统（如 winston）
4. 考虑添加 TypeScript 支持
5. 添加 API 文档（如 Swagger）
6. 实现更细粒度的错误处理
