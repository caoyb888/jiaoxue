#!/usr/bin/env bash
# 快速查看所有服务端口状态
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'

chk() {
  local label=$1 port=$2
  if ss -tlnp 2>/dev/null | grep -q ":${port} "; then
    echo -e "  ${GREEN}UP${NC}   $label (port $port)"
  else
    echo -e "  ${RED}DOWN${NC} $label (port $port)"
  fi
}

echo "=== 中间件 ==="
chk "MySQL"          13306
chk "Redis"          16379
chk "Nacos"          18848
chk "Kafka"          19092
chk "MongoDB"        17017
chk "MinIO"          19001
chk "Elasticsearch"  19200
chk "ClickHouse"     18123
chk "XXL-Job"        18160

echo ""
echo "=== 微服务 ==="
chk "edu-gateway"     18080
chk "edu-auth"         8081
chk "edu-user"        18082
chk "edu-course"       8083
chk "edu-interaction"  8084
chk "edu-exam"         8085
chk "edu-grade"        8086
chk "edu-ai"           8087
chk "edu-stat"        18088
chk "edu-file"         8089
chk "edu-notify"       8090
chk "edu-live"         8091
chk "edu-admin"        8092
chk "edu-jwxt"         8093

echo ""
echo "=== 前端 ==="
chk "web (Vite)"     5173
