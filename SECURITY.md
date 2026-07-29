# 安全策略

## 支持的版本

| 版本 | 支持状态 |
|------|---------|
| 最新 Release | ✅ 积极维护 |
| 其他版本 | ❌ 不再支持 |

请始终使用 [最新发布版本](https://github.com/mvpflk/teaching-system/releases)。

## 报告安全漏洞

如果你发现了安全漏洞（如 SQL 注入、XSS、权限绕过、敏感数据泄露等），**请不要公开提交 Issue**。

请通过以下方式之一私下报告：

1. **GitHub Security Advisory**（推荐）
   - 访问 https://github.com/mvpflk/teaching-system/security/advisories
   - 点击 "New advisory" 提交

2. **发送邮件**
   - 在仓库主页找到项目维护者的联系方式
   - 邮件主题请以 `[Security]` 开头

### 报告时请包含

- 漏洞类型和严重程度
- 完整的复现步骤
- 受影响的版本
- 如果可能，提供修复建议

### 响应时间

- 我们会在 **48 小时内** 确认收到报告
- **7 天内** 给出初步评估和修复计划
- 修复完成并发布后，会公开致谢（如果你同意）

## 安全最佳实践（部署者必读）

### 部署前必须修改

| 配置项 | 说明 |
|--------|------|
| `JWT_SECRET` | 至少 32 字符随机字符串，`openssl rand -base64 32` 生成 |
| `MYSQL_ROOT_PASSWORD` | 数据库 root 密码 |
| `DB_APP_PASSWORD` | 应用数据库密码 |
| `REDIS_PASSWORD` | Redis 密码 |
| `CORS_ORIGINS` | 改为你的实际域名 |

### 生产环境建议

- 始终使用 HTTPS（推荐 Let's Encrypt 免费证书）
- 将 Nginx 的 `listen 80` 改为 443 并配置 SSL
- 不要在公网暴露 MySQL 端口（3306/3307）
- 定期备份数据库
- 保持服务器系统和 Docker 镜像更新

## 已知安全措施（本项目的防护）

- JWT 认证 + 黑名单机制
- 密码加密存储（BCrypt）
- CORS 域名白名单
- X-Content-Type-Options / X-Frame-Options / Referrer-Policy 安全头
- SQL 注入防护（MyBatis-Plus 参数绑定）
- AI API 调用速率限制
- 登录频率限制

## 漏洞披露历史

此部分记录公开披露的安全相关修复。

| 日期 | 编号 | 类型 | 致谢 |
|------|------|------|------|
| - | - | - | - |
