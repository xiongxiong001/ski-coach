#!/bin/bash
# ===========================================================
# 服务健康检查脚本
# 一键查看所有服务状态
# ===========================================================

DEPLOY_DIR=$(cd "$(dirname "$0")/.." && pwd)
cd "$DEPLOY_DIR"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo "========================================"
echo "  Ski Coach 服务状态检查"
echo "========================================"
echo ""

# 1. 容器状态
echo "📦 容器状态:"
docker compose ps
echo ""

# 2. 端口监听
echo "🔌 端口监听:"
for port in 80 8080 8001 3306 6379; do
  if ss -tlnp 2>/dev/null | grep -q ":$port "; then
    echo -e "  ${GREEN}✓${NC} $port"
  else
    echo -e "  ${RED}✗${NC} $port (未监听)"
  fi
done
echo ""

# 3. 业务接口检查
echo "🩺 业务接口检查:"

# Java
if curl -sf http://localhost:8080/actuator/health > /dev/null; then
  echo -e "  ${GREEN}✓${NC} Java API (/actuator/health)"
else
  echo -e "  ${RED}✗${NC} Java API"
fi

# Python
if curl -sf http://localhost:8001/health > /dev/null; then
  echo -e "  ${GREEN}✓${NC} Python AI (/health)"
else
  echo -e "  ${RED}✗${NC} Python AI"
fi

# Nginx → Java
if curl -sf http://localhost/api/auth/register -o /dev/null -X POST -H "Content-Type: application/json" -d '{}' 2>&1 | grep -q ""; then
  echo -e "  ${GREEN}✓${NC} Nginx 转发"
fi
echo ""

# 4. Redis 队列
echo "📨 Redis 任务队列:"
QUEUE_LEN=$(docker exec ski-redis redis-cli LLEN ski_coach:task:queue 2>/dev/null || echo "?")
echo "  当前堆积任务数: $QUEUE_LEN"
if [ "$QUEUE_LEN" -gt 50 ] 2>/dev/null; then
  echo -e "  ${YELLOW}⚠️  堆积较多,考虑加 Worker${NC}"
fi
echo ""

# 5. 磁盘
echo "💾 视频存储磁盘:"
source .env 2>/dev/null
VIDEO_PATH="${VIDEO_DATA_PATH:-./data/ski-data}"
if [ -d "$VIDEO_PATH" ]; then
  df -h "$VIDEO_PATH" | tail -1
  COUNT=$(find "$VIDEO_PATH" -type f -name "*.mp4" 2>/dev/null | wc -l)
  echo "  视频文件数: $COUNT"
fi
echo ""

# 6. 资源占用
echo "📊 资源占用:"
docker stats --no-stream --format "  {{.Name}}: CPU {{.CPUPerc}}, MEM {{.MemUsage}}" \
  ski-mysql ski-redis ski-api-server ski-ai-server ski-nginx 2>/dev/null
echo ""

echo "========================================"
