#!/bin/bash
# ============================================
# 教学管理系统 — 数据库备份脚本
# 用法:
#   手动备份:  ./tools/backup-db.sh
#   设为定时:  crontab -e
#             0 3 * * * /home/ubuntu/teaching-system/tools/backup-db.sh
# ============================================

set -e

# ── 配置（根据你的 .env 修改密码） ──
DB_USER="teaching_app"
DB_PASS="Lm19821227"        # ← 改成你的实际密码
DB_NAME="teaching_system"
CONTAINER="teaching-mysql"
BACKUP_DIR="/root/backups"
RETENTION_DAYS=30

# ── 创建备份目录 ──
mkdir -p "$BACKUP_DIR"

# ── 备份文件名：teaching_20260729.sql.gz ──
FILE="$BACKUP_DIR/teaching_$(date +%Y%m%d_%H%M%S).sql.gz"

# ── 执行备份（先压缩再写入，减少磁盘IO） ──
echo "[$(date '+%Y-%m-%d %H:%M:%S')] 开始备份 $DB_NAME ..."
docker exec "$CONTAINER" mysqldump \
    -u "$DB_USER" \
    -p"$DB_PASS" \
    --single-transaction \
    --quick \
    --routines \
    --triggers \
    --events \
    "$DB_NAME" | gzip > "$FILE"

# ── 验证备份是否成功 ──
if [ -s "$FILE" ]; then
    SIZE=$(du -h "$FILE" | cut -f1)
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✅ 备份完成: $FILE ($SIZE)"
else
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ❌ 备份失败: 文件为空"
    rm -f "$FILE"
    exit 1
fi

# ── 删除超过 30 天的旧备份 ──
find "$BACKUP_DIR" -name "teaching_*.sql.gz" -mtime +$RETENTION_DAYS -delete
echo "[$(date '+%Y-%m-%d %H:%M:%S')] 已清理 $RETENTION_DAYS 天前的旧备份"
