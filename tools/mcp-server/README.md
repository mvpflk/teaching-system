# 教学系统 MCP 服务器

为教学管理系统提供数据库查询、文件系统、API集成、教学专用功能的 Model Context Protocol 服务器。

## 功能模块

### 数据库工具 (db_*)
- `db_query` - 执行 SQL 查询
- `db_execute` - 执行 SQL 更新
- `db_tables` - 获取所有表名
- `db_schema` - 获取表结构
- `db_count` - 统计记录数

### 文件系统工具 (file_*)
- `file_read` - 读取文件
- `file_write` - 写入文件
- `file_list` - 列出目录
- `file_search` - 搜索文件
- `file_info` - 获取文件信息

### API 集成工具 (api_*)
- `api_request` - 通用 HTTP 请求
- `api_github` - GitHub API
- `api_gitlab` - GitLab API
- `api_fetch` - 抓取网页内容

### 教学专用工具 (teaching_*)
- `teaching_students` - 学生列表
- `teaching_teachers` - 教师列表
- `teaching_classes` - 班级列表
- `teaching_tasks` - 任务列表
- `teaching_questions` - 题库搜索
- `teaching_knowledge_tree` - 知识树
- `teaching_ai_outputs` - AI 生成内容
- `teaching_credit_stats` - 积分统计

## 安装

```bash
cd tools/mcp-server
npm install
npm run build
```

## 配置

复制 `.env.example` 为 `.env` 并填入数据库连接信息：

```bash
cp .env.example .env
# 编辑 .env 文件
```

## 运行

```bash
# 直接运行
npm start

# 开发模式
npm run dev

# 使用 MCP Inspector 调试
npm run inspector
```

## 在 Claude Code 中配置

在 `.mcp.json` 中添加：

```json
{
  "mcpServers": {
    "teaching-system": {
      "command": "node",
      "args": ["tools/mcp-server/dist/index.js"],
      "env": {
        "DB_HOST": "localhost",
        "DB_PASSWORD": "root123"
      }
    }
  }
}
```

## 资源

服务器还提供以下资源：

- `teaching://database/tables` - 数据库表结构
- `teaching://config/env` - 环境配置信息