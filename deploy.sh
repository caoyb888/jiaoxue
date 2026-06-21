#!/usr/bin/env bash
# ============================================================
# 本机一键部署脚本：推送代码 → 内网机启动
# 在本机（开发机）执行：bash deploy.sh [选项]
#   --skip-build     内网机跳过 Maven 编译
#   --skip-seed      内网机跳过测试数据注入
#   --backend-only   只启动后端
#   --frontend-only  只推送并启动前端
# ============================================================
set -e

BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "main")
REMOTE="onlyserver"
SCRIPT="~/smart-edu/infra/scripts/start-all.sh"
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'

# 收集透传参数
PASS_ARGS=""
for arg in "$@"; do PASS_ARGS="$PASS_ARGS $arg"; done

echo -e "${GREEN}[DEPLOY]${NC} 当前分支：$BRANCH"
echo -e "${GREEN}[DEPLOY]${NC} 目标服务器：onlyserver (100.84.68.115)"

# 推送代码
echo -e "\n${GREEN}[DEPLOY]${NC} 推送代码到内网机..."
git push "$REMOTE" "$BRANCH" --force-with-lease

# 触发内网机启动
echo -e "\n${GREEN}[DEPLOY]${NC} 触发内网机启动脚本..."
ssh onlyserver "cd ~/smart-edu && git checkout $BRANCH && bash $SCRIPT $PASS_ARGS"
