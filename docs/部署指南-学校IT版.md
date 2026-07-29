# 教学管理系统 — 部署指南（学校 IT 版）

> 适用对象：学校信息技术老师（会基础 Linux 命令即可，不需要会 Java 或编程）
> 部署时间：约 30 分钟

---

## 一、你需要准备的东西

| 项目 | 要求 | 说明 |
|------|------|------|
| **一台服务器** | 2 核 CPU、4GB 内存、50GB 磁盘 | 腾讯云/阿里云/学校自有服务器均可 |
| **操作系统** | Ubuntu 22.04 或 CentOS 7+ | 推荐 Ubuntu 22.04 |
| **公网 IP** | 可选，内网部署也行 | 看学校是否需要校外访问 |
| **域名** | 可选 | 如果不配域名，用 IP 地址访问即可 |

---

## 二、一、安装 Docker

**如果服务器上已经装过 Docker，跳过这一步。**

登录服务器（用 SSH 工具如 Xshell / Putty / 终端），逐条执行以下命令：

```bash
# 1. 安装 Docker（Ubuntu）
curl -fsSL https://get.docker.com | bash

# 2. 安装 Docker Compose（单独安装方式）
sudo apt install docker-compose -y

# 3. 将当前用户加入 docker 组（避免每次输入 sudo）
sudo usermod -aG docker $USER

# 4. 退出重新登录使组生效
exit
```

重新登录后验证安装：

```bash
docker --version
docker-compose --version
```

两条命令都有版本号输出，说明安装成功。

---

## 三、下载项目

```bash
# 1. 创建工作目录
mkdir -p /home/ubuntu/teaching-system
cd /home/ubuntu/teaching-system

# 2. 从你的代码仓库下载（二选一）

# 方式 A：如果有 Git 仓库
git clone https://github.com/你的用户名/teaching-system.git .

# 方式 B：如果是压缩包（把 zip 或 tar.gz 上传到服务器后）
# 先在本地把项目打包，用 scp 传到服务器
# scp teaching-system.tar.gz root@你的服务器IP:/home/ubuntu/
# 然后在服务器上：
# tar -xzf /home/ubuntu/teaching-system.tar.gz -C /home/ubuntu/teaching-system
```

---

## 四、配置环境变量

```bash
# 1. 复制环境变量模板
cp backend/.env.example .env

# 2. 编辑 .env 文件
nano .env
```

修改以下**必填**项目（其他保持默认即可）：

```bash
# 数据库密码：自己设一个，记下来
DB_PASSWORD=你的数据库密码（如 your-db-password）

# JWT密钥：运行下面命令生成
# 在终端执行： openssl rand -base64 32
# 把输出的字符串粘贴到这里
JWT_SECRET=这里粘贴生成的密钥

# 前端访问地址（你的服务器IP或域名）
CORS_ORIGINS=http://你的服务器IP:3000

# DeepSeek AI 的 API Key（如果要用AI功能）
DEEPSEEK_API_KEY=sk-你的DeepSeek密钥
```

> ⚠️ **请务必修改默认密码**，不要直接用示例中的密码。

编辑完成后按 `Ctrl+X`，按 `Y`，按 `Enter` 保存退出。

---

## 五、初始化数据库

```bash
# 1. 复制数据库初始化脚本到工作目录
# init.sql 已经在 database/ 目录下了，docker-compose 会自动加载它

# 2. 创建数据目录（存放上传的文件和日志）
mkdir -p data/uploads data/logs data/mysql data/redis
```

---

## 六、启动系统

```bash
# 首次启动（下载镜像+创建数据库+启动所有服务）
# 这一步需要联网下载镜像，耗时 5-15 分钟，请耐心等待
docker-compose up -d

# 查看启动进度
docker-compose logs -f
```

看到以下输出说明启动成功：

```
teaching-mysql    | ready for connections
teaching-backend  | Started TeachingSystemApplication in XX seconds
teaching-frontend | /docker-entrypoint.sh: /docker-entrypoint.d/... done
```

按 `Ctrl+C` 退出日志查看。

---

## 七、验证系统是否正常运行

```bash
# 检查所有容器是否都在运行
docker ps

# 应该看到 4 个容器都是 "Up" 状态：
# - teaching-mysql
# - teaching-redis
# - teaching-backend
# - teaching-frontend

# 检查后端健康状态
curl http://localhost:8080/api/health

# 应该返回：{"code":200,"msg":"成功","data":{"status":"UP",...}}

# 检查前端是否可访问
curl http://localhost
# 应该返回 HTML 内容（不是错误）
```

**浏览器访问：**

```
http://你的服务器IP
```

看到登录页面，说明部署成功！

---

## 八、首次登录

| 角色 | 账号 | 密码 |
|------|------|------|
| 系统管理员 | `admin` | `admin123` |

> ⚠️ **首次登录后请务必修改 admin 密码！**

---

## 九、日常维护

### 9.1 查看运行状态

```bash
docker ps                    # 看所有容器是否正常
docker-compose logs -f       # 看实时日志
docker-compose logs backend  # 只看后端日志
```

### 9.2 重启服务

```bash
docker-compose restart backend   # 只重启后端
docker-compose restart frontend  # 只重启前端
docker-compose restart           # 重启所有服务
```

### 9.3 更新代码

```bash
cd /home/ubuntu/teaching-system

# 1. 拉取最新代码（如果是 Git 方式）
git pull

# 2. 如果你不会用 Git，直接把新的压缩包解压覆盖

# 3. 重新部署
./deploy.sh all --skip-review
```

### 9.4 备份数据库（重要！）

```bash
# 手动备份（备份文件会生成在 data/backup/ 目录）
docker exec teaching-mysql mysqldump -u teaching_app -p你的密码 teaching_system > data/backup/teaching_$(date +%Y%m%d_%H%M%S).sql

# 自动备份（将上面命令加到 crontab 中，每天凌晨执行）
# crontab -e
# 0 3 * * * docker exec teaching-mysql mysqldump -u teaching_app -p你的密码 teaching_system > /home/ubuntu/teaching-system/data/backup/teaching_$(date +\%Y\%m\%d).sql
```

### 9.5 恢复数据库

```bash
# 从备份文件恢复
cat data/backup/teaching_20260701.sql | docker exec -i teaching-mysql mysql -u teaching_app -p你的密码 teaching_system
```

---

## 十、常见问题

### Q：启动后浏览器访问显示 "502 Bad Gateway"

**原因：** 后端还没完全启动（首次启动需要 30-60 秒）。
**解决：** 等 1 分钟后刷新页面。如果仍然 502，执行：

```bash
docker-compose logs backend | tail -30
```

把输出结果截图发给开发者。

### Q：数据库连接失败

**原因：** 环境变量配置错误。
**解决：** 检查 `.env` 文件中的 `DB_PASSWORD` 是否和 `docker-compose.yml` 中的一致。

### Q：所有容器都正常，但登录后是白屏

**原因：** 前端请求后端地址配置不对。
**解决：** 检查 `.env` 中的 `CORS_ORIGINS` 是否填写了正确的访问地址。

### Q：想用 HTTPS（配域名）

**方案 A：用 Nginx 反向代理（推荐）**

```bash
# 安装 nginx
sudo apt install nginx -y

# 编辑配置
sudo nano /etc/nginx/sites-available/teaching
```

粘贴以下内容（将 `your.domain.com` 替换为你的域名）：

```nginx
server {
    listen 443 ssl;
    server_name your.domain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://localhost:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}

server {
    listen 80;
    server_name your.domain.com;
    return 301 https://$server_name$request_uri;
}
```

```bash
sudo ln -s /etc/nginx/sites-available/teaching /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

**方案 B：用 Cloudflare Tunnel（免费，无需公网 IP）**

在 Cloudflare 面板开启 Tunnel 功能，指向 `http://localhost:80` 即可。

### Q：服务器重启后系统会自动启动吗？

会的。Docker 容器的 `restart: always` 策略保证服务器重启后自动拉起所有服务。

### Q：磁盘空间不够怎么办？

```bash
# 查看磁盘使用情况
df -h

# 清理 Docker 不用的镜像和缓存（不会影响系统运行）
docker system prune -af

# 清理历史日志（保留最近 7 天）
docker-compose logs --tail=0 2>/dev/null
find /var/lib/docker/containers/ -name "*.log" -exec truncate -s 0 {} \;
```

---

## 十一、系统架构（选读）

```
用户浏览器 → Nginx (端口 80) → 前端静态页面
               └→ /api/* → Spring Boot 后端 (端口 8080) → MySQL (端口 3307)
                                                            └→ Redis (缓存/限流)
```

所有组件都在 Docker 容器中运行：

| 容器名 | 用途 | 资源上限 |
|--------|------|---------|
| teaching-mysql | 数据库 | 1.5GB 内存, 1 核 CPU |
| teaching-redis | 缓存 | 384MB 内存, 0.5 核 CPU |
| teaching-backend | Java 后端 | 1.5GB 内存, 1 核 CPU |
| teaching-frontend | Nginx 前端 | 128MB 内存, 0.3 核 CPU |

---

## 附录：一键部署命令速查

```bash
# === 安装 ===
curl -fsSL https://get.docker.com | bash
sudo usermod -aG docker $USER && exit

# === 配置 ===
cp backend/.env.example .env
nano .env   # 修改 DB_PASSWORD、JWT_SECRET、CORS_ORIGINS

# === 启动 ===
mkdir -p data/uploads data/logs
docker-compose up -d
docker ps

# === 访问 ===
echo "打开浏览器访问 http://$(curl -s ifconfig.me)"
echo "账号: admin  密码: admin123"

# === 备份 ===
docker exec teaching-mysql mysqldump -u teaching_app -p你的密码 teaching_system > teaching_$(date +%Y%m%d).sql

# === 升级 ===
git pull && docker-compose up -d --build
```
