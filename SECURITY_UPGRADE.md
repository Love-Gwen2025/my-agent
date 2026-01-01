# 安全优化升级指南

本文档说明如何应用最近的安全优化改进。

## 修复内容

### 1. 数据库连接池优化 ✅
**问题**: 连接池配置过小（min=2, max=10），高并发下导致连接等待超时。

**修复**:
- 最小连接数: 2 → 5
- 最大连接数: 10 → 20
- 添加连接超时: 30秒
- 改进日志输出

**影响**: 无需额外操作，重启服务即生效。

---

### 2. API Key 加密算法改进 ✅
**问题**: 使用 SHA-256 派生密钥，不符合安全标准。

**修复**:
- 使用 PBKDF2 密钥派生函数
- 添加固定盐值
- 100,000 次迭代增加破解难度

**影响**: 需要迁移已有的加密数据。

#### 迁移步骤

**⚠️ 重要：迁移前请备份数据库！**

```bash
# 1. 备份数据库
pg_dump -U postgres -d my_agent > backup_$(date +%Y%m%d_%H%M%S).sql

# 2. 运行迁移脚本
cd backend
uv run python scripts/migrate_crypto.py

# 3. 按提示输入 'yes' 确认迁移

# 4. 验证迁移结果
# 脚本会自动验证所有 API Key 是否能正常解密
```

**迁移脚本说明**:
- 自动检测旧算法加密的数据
- 用旧算法解密 → 用新算法重新加密
- 验证所有数据迁移成功
- 跳过已迁移或无效的数据

**回滚方案**:
如果迁移失败，可以恢复备份：
```bash
psql -U postgres -d my_agent < backup_YYYYMMDD_HHMMSS.sql
```

---

### 3. Token 存储安全改进 ✅
**问题**: Token 存储在 localStorage，易受 XSS 攻击。

**修复**:
- 后端通过 HttpOnly Cookie 返回 Token
- Cookie 设置 Secure 和 SameSite 属性
- 前端自动发送 Cookie，无需手动处理
- 保持向后兼容（支持旧的 Header 方式）

**影响**: 无需额外操作，但建议清理旧数据。

#### 用户端操作（可选）

用户可以手动清理浏览器中的旧 Token：

1. 打开浏览器开发者工具（F12）
2. 进入 Application/Storage → Local Storage
3. 删除 `token` 项（保留 `isLoggedIn` 标记）
4. 重新登录

**注意**: 新登录的用户会自动使用 HttpOnly Cookie，无需手动操作。

---

## 技术细节

### Token 认证优先级

后端现在支持三种认证方式（优先级从高到低）：

1. **Authorization Header** (Bearer Token)
   ```
   Authorization: Bearer <token>
   ```

2. **token Header**
   ```
   token: <token>
   ```

3. **token Cookie** (HttpOnly，推荐)
   ```
   Cookie: token=<token>
   ```

### Cookie 安全属性

```python
response.set_cookie(
    key="token",
    value=token,
    httponly=True,   # 防止 JavaScript 访问
    secure=True,     # 仅 HTTPS 传输
    samesite="lax",  # 防止 CSRF
    max_age=86400,   # 24小时过期
    path="/",
)
```

### 前端配置

```typescript
// axios 配置
const apiClient = axios.create({
  withCredentials: true,  // 允许发送 Cookie
});
```

---

## 验证修复

### 1. 验证连接池配置

查看应用启动日志：
```
Checkpointer pool created: min_size=5, max_size=20, timeout=30s, health_check=enabled
```

### 2. 验证 API Key 加密

运行迁移脚本后，检查输出：
```
✅ 所有 API Key 验证成功！迁移完成。
```

### 3. 验证 Token Cookie

1. 登录系统
2. 打开浏览器开发者工具 → Network
3. 查看登录请求的响应头：
   ```
   Set-Cookie: token=...; HttpOnly; Secure; SameSite=Lax; Max-Age=86400; Path=/
   ```
4. 后续请求会自动携带 Cookie

---

## 常见问题

### Q1: 迁移脚本报错 "无法用旧算法解密"
**A**: 这是正常的，说明该数据已经是新算法加密或数据损坏。脚本会跳过这些数据。

### Q2: 本地开发环境 Cookie 不生效
**A**: 本地开发时，将 `secure=True` 改为 `secure=False`（仅开发环境）：
```python
# backend/app/api/routes/user.py
response.set_cookie(
    ...
    secure=False,  # 本地开发
)
```

### Q3: 跨域请求 Cookie 不发送
**A**: 确保：
1. 前端配置 `withCredentials: true`
2. 后端 CORS 配置允许凭证：
   ```python
   app.add_middleware(
       CORSMiddleware,
       allow_credentials=True,
   )
   ```

### Q4: 用户需要重新登录吗？
**A**: 不需要。系统会自动兼容旧的认证方式，用户下次登录时会自动切换到新方式。

---

## 性能影响

| 优化项 | 性能影响 | 说明 |
|--------|---------|------|
| 连接池扩容 | **+100%** 并发能力 | 支持 20 并发连接 |
| PBKDF2 加密 | 首次加密 +50ms | 仅影响用户模型创建/更新 |
| HttpOnly Cookie | 无影响 | Cookie 大小与 Header 相同 |

---

## 安全提升

| 优化项 | 安全等级 | 防护能力 |
|--------|---------|---------|
| PBKDF2 加密 | ⭐⭐⭐⭐⭐ | 防止 API Key 暴力破解 |
| HttpOnly Cookie | ⭐⭐⭐⭐⭐ | 防止 XSS 窃取 Token |
| SameSite Cookie | ⭐⭐⭐⭐ | 防止 CSRF 攻击 |
| 连接超时 | ⭐⭐⭐ | 防止连接耗尽 DoS |

---

## 后续建议

1. **监控连接池使用率**
   - 添加 Prometheus 指标
   - 设置告警阈值（80% 使用率）

2. **定期轮换 JWT Secret**
   - 建议每季度更换一次
   - 更换后需要用户重新登录

3. **启用 HTTPS**
   - 生产环境必须启用
   - 使用 Let's Encrypt 免费证书

4. **添加登录限流**
   - 防止暴力破解
   - 建议 5 次失败后锁定 30 分钟

---

## 联系支持

如有问题，请：
1. 查看应用日志：`docker compose logs -f backend`
2. 提交 Issue 到项目仓库
3. 附上错误日志和环境信息
