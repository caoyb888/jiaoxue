#!/usr/bin/env bash
# 停止所有微服务和前端（不停 Docker 中间件）
PID_DIR="$HOME/.edu-dev/pids"
GREEN='\033[0;32m'; RED='\033[0;31m'; NC='\033[0m'

echo "正在停止所有服务..."

for pid_file in "$PID_DIR"/*.pid; do
  [ -f "$pid_file" ] || continue
  name=$(basename "$pid_file" .pid)
  pid=$(cat "$pid_file")
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid" 2>/dev/null
    echo -e "  ${GREEN}✓${NC} $name（PID=$pid）已停止"
  else
    echo -e "  ${RED}-${NC} $name（PID=$pid）已不在运行"
  fi
  rm -f "$pid_file"
done

echo "所有服务已停止。Docker 中间件仍在运行。"
