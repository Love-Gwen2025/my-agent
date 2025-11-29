## 1. Implementation
- [ ] 1.1 梳理现有 SQLite3 使用点（业务数据与 connect-sqlite3 session），确认需要替换的模块与启动顺序。
- [ ] 1.2 新增 MySQL 连接配置（env & config），实现连接池初始化与启动期健康检查，替换 db 访问层依赖。
- [ ] 1.3 用 MySQL 查询接口改写 db/connection 与 db/queries，保持现有 API 形态与事务/错误处理语义。
- [ ] 1.4 将 session 存储切换为 MySQL（包含 session 表结构、过期策略与清理任务），移除 sqlite session 依赖。
- [ ] 1.5 提供 MySQL 建表语句（users/conversations/messages/attachments/sessions），并在仓库内落地为可执行脚本或文档。
- [ ] 1.6 设计并实现从 SQLite3 导出/导入到 MySQL 的迁移路径，覆盖数据校验与回滚方案。
- [ ] 1.7 更新部署/本地运行文档与环境变量说明，移除 SQLite 相关指引。
- [ ] 1.8 编写或更新验证步骤（本地启动、核心 API 冒烟、会话保持），确保迁移后功能不回退。
