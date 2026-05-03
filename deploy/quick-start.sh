#!/bin/bash
# ===========================================================
# 本地快速启动脚本(开发用)
#
# 一键执行:
#   1. 检查 Docker 是否安装
#   2. 创建 .env 如不存在
#   3. 构建镜像
#   4. 启动所有服务
#   5. 等待健康检查通过
#   6. 显示访问地址
# ===========================================================

set -e

DEPLOY_DIR=$(cd "$(dirname "$0")" && pwd)
cd "$DEPLOY_DIR"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log() { echo -e "${GREEN}[$(date '+%H:%M:%S')] $1${NC}"; }
warn() { echo -e "${YELLOW}[$(date '+%H:%M:%S')] $1${NC}"; }
err() { echo -e "${RED}[$(date '+%H:%M:%S')] $1${NC}"; exit 1; }

# === 1. 检查环境 ===
log "🔍 检查环境..."
command -v docker >/dev/null 2>&1 || err "请先安装 Docker"
docker compose version >/dev/null 2>&1 || err "请先安装 docker compose 插件"
log "✅ Docker 已就绪"

# === 2. 准备 .env ===
if [ ! -f .env ]; then
  warn "⚠️  .env 不存在,从 .env.example 创建..."
  cp .env.example .env

  # 生成随机 JWT 密钥
  JWT_USER=$(openssl rand -hex 32 2>/dev/null || head -c 32 /dev/urandom | base64)
  JWT_ADMIN=$(openssl rand -hex 32 2>/dev/null || head -c 32 /dev/urandom | base64)

  # 替换默认值(macOS / Linux 兼容)
  if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' "s|JWT_USER_SECRET=.*|JWT_USER_SECRET=$JWT_USER|" .env
    sed -i '' "s|JWT_ADMIN_SECRET=.*|JWT_ADMIN_SECRET=$JWT_ADMIN|" .env
  else
    sed -i "s|JWT_USER_SECRET=.*|JWT_USER_SECRET=$JWT_USER|" .env
    sed -i "s|JWT_ADMIN_SECRET=.*|JWT_ADMIN_SECRET=$JWT_ADMIN|" .env
  fi

  warn "⚠️  .env 已创建,JWT 密钥已自动生成"
  warn "⚠️  请编辑 .env 修改 DEEPSEEK_API_KEY,然后再次运行此脚本"
  echo ""
  echo "  vim .env"
  exit 0
fi

# === 3. 校验关键配置 ===
source .env
if [[ "$DEEPSEEK_API_KEY" == "sk-xxxxxxxxxxxxxxxxxxxxxxxx" ]] || [[ -z "$DEEPSEEK_API_KEY" ]]; then
  err "❌ DEEPSEEK_API_KEY 未配置,请编辑 .env"
fi

# === 4. 创建视频目录 ===
VIDEO_PATH="${VIDEO_DATA_PATH:-./data/ski-data}"
mkdir -p "$VIDEO_PATH"
log "📁 视频存储目录: $VIDEO_PATH"

# === 5. 构建 ===
log "🔨 构建镜像(首次较慢,请耐心等待)..."
chmod +x build.sh
./build.sh all

# === 6. 启动 ===
log "🚀 启动所有服务..."
docker compose up -d

# === 7. 等待健康检查 ===
log "⏳ 等待服务健康..."
MAX_WAIT=60
WAITED=0
while [ $WAITED -lt $MAX_WAIT ]; do
  if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    log "✅ Java API 已就绪"
    break
  fi
  sleep 2
  WAITED=$((WAITED + 2))
  echo -n "."
done
echo ""

if [ $WAITED -ge $MAX_WAIT ]; then
  warn "⚠️  Java API 启动较慢,看下日志:docker compose logs ski-api-server"
fi

# === 8. 完成 ===
echo ""
echo "========================================"
echo "  🎉 启动完成!"
echo "========================================"
echo ""
echo "📱 用户端 H5:    http://localhost"
echo "🔧 管理后台:     http://admin.localhost"
echo "    (本地需在 hosts 加: 127.0.0.1 admin.localhost)"
echo "🔬 后端 API 文档: http://localhost:8080/doc.html (生产关闭)"
echo ""
echo "默认管理员账号: admin / admin123"
echo ""
echo "查看日志: docker compose logs -f ski-api-server"
echo "停止服务: docker compose down"
echo "完整状态: ./scripts/check-status.sh"
echo ""
