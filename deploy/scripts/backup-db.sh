#!/bin/bash
# ===========================================================
# 数据库备份脚本
# 使用:加到 crontab,每天凌晨 2 点跑
#   0 2 * * * /home/ski/ski-coach/deploy/scripts/backup-db.sh
# ===========================================================

set -e

DEPLOY_DIR=$(cd "$(dirname "$0")/.." && pwd)
BACKUP_DIR="${DEPLOY_DIR}/backup/db"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p "$BACKUP_DIR"

# 加载环境变量
source "$DEPLOY_DIR/.env"

# 备份
docker exec ski-mysql mysqldump \
  -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction --quick \
  ski_coach > "$BACKUP_DIR/ski_coach_${DATE}.sql"

# 压缩
gzip "$BACKUP_DIR/ski_coach_${DATE}.sql"

# 删除 30 天前的备份
find "$BACKUP_DIR" -name "ski_coach_*.sql.gz" -mtime +30 -delete

echo "[$(date)] 备份完成: $BACKUP_DIR/ski_coach_${DATE}.sql.gz"
