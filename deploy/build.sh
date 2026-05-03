#!/bin/bash
# ===========================================================
# Ski Coach 镜像构建脚本
#
# 用法:
#   ./build.sh           构建所有
#   ./build.sh api       只构建 Java
#   ./build.sh ai        只构建 Python
#   ./build.sh web       只构建前端(打包静态文件到 web/)
# ===========================================================

set -e

# 项目根目录(deploy 的上级)
DEPLOY_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT_DIR=$(cd "$DEPLOY_DIR/.." && pwd)

cd "$DEPLOY_DIR"

# 颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log() { echo -e "${GREEN}[$(date '+%H:%M:%S')] $1${NC}"; }
warn() { echo -e "${YELLOW}[$(date '+%H:%M:%S')] $1${NC}"; }
err() { echo -e "${RED}[$(date '+%H:%M:%S')] $1${NC}"; exit 1; }

build_api() {
  log "🔨 构建 ski-api-server 镜像..."
  if [ ! -d "$ROOT_DIR/ski-api-server" ]; then
    err "找不到 $ROOT_DIR/ski-api-server"
  fi
  cp dockerfiles/Dockerfile.api "$ROOT_DIR/ski-api-server/Dockerfile"
  docker build -t ski-api-server:latest "$ROOT_DIR/ski-api-server"
  rm "$ROOT_DIR/ski-api-server/Dockerfile"
  log "✅ ski-api-server 构建完成"
}

build_ai() {
  log "🔨 构建 ski-ai-server 镜像..."
  if [ ! -d "$ROOT_DIR/ski-ai-server" ]; then
    err "找不到 $ROOT_DIR/ski-ai-server"
  fi
  cp dockerfiles/Dockerfile.ai "$ROOT_DIR/ski-ai-server/Dockerfile"
  docker build -t ski-ai-server:latest "$ROOT_DIR/ski-ai-server"
  rm "$ROOT_DIR/ski-ai-server/Dockerfile"
  log "✅ ski-ai-server 构建完成"
}

build_web() {
  log "🔨 构建前端静态文件..."
  mkdir -p "$DEPLOY_DIR/web"

  # 移动端
  if [ -d "$ROOT_DIR/ski-mobile-web" ]; then
    log "📦 编译 ski-mobile-web..."
    cd "$ROOT_DIR/ski-mobile-web"
    if [ ! -d node_modules ]; then npm ci || npm install; fi
    npm run build
    rm -rf "$DEPLOY_DIR/web/mobile"
    cp -r dist "$DEPLOY_DIR/web/mobile"
    log "✅ mobile 静态文件已放到 deploy/web/mobile"
    cd "$DEPLOY_DIR"
  else
    warn "⚠️ 跳过 ski-mobile-web(目录不存在)"
  fi

  # 管理后台
  if [ -d "$ROOT_DIR/ski-admin-web" ]; then
    log "📦 编译 ski-admin-web..."
    cd "$ROOT_DIR/ski-admin-web"
    if [ ! -d node_modules ]; then npm ci || npm install; fi
    npm run build
    rm -rf "$DEPLOY_DIR/web/admin"
    cp -r dist "$DEPLOY_DIR/web/admin"
    log "✅ admin 静态文件已放到 deploy/web/admin"
    cd "$DEPLOY_DIR"
  else
    warn "⚠️ 跳过 ski-admin-web(目录不存在)"
  fi
}

case "${1:-all}" in
  api)  build_api ;;
  ai)   build_ai ;;
  web)  build_web ;;
  all)
    build_api
    build_ai
    build_web
    log "🎉 全部构建完成"
    ;;
  *)
    err "用法: $0 [api|ai|web|all]"
    ;;
esac
