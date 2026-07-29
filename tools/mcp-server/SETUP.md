# MCP 服务器配置完成

## 已配置的 MCP 服务

### 1. 教学系统 MCP 服务器 (teaching-system)

提供 22 个工具，涵盖：
- **数据库查询** (db_*): SQL查询、表结构、统计
- **文件系统** (file_*): 文件读写、目录浏览、搜索
- **API 集成** (api_*): HTTP请求、GitHub/GitLab API、网页抓取
- **教学专用** (teaching_*): 学生/教师/班级/任务/题库/知识树等

### 2. Playwright MCP (playwright)

浏览器自动化测试工具。

## 快速开始

```bash
# 1. 进入 MCP 服务器目录
cd tools/mcp-server

# 2. 安装依赖（已完成）
npm install

# 3. 构建（已完成）
npm run build

# 4. 配置环境变量
cp .env.example .env
# 编辑 .env 填入数据库密码

# 5. 测试服务器
npm run inspector
```

## 在 Claude Code 中使用

MCP 服务器已配置在 `.mcp.json` 中，重启 Claude Code 后自动加载。

使用示例：
```
请查询数据库中所有学生信息
→ 自动调用 teaching_students 工具

请读取 backend/pom.xml 文件
→ 自动调用 file_read 工具

请查询 GitHub 上的 issues
→ 自动调用 api_github 工具
```