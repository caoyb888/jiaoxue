#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# S6-15 为 edu_ai 库创建/校验 TTL 索引（幂等）。
# 在内网机执行：bash ~/smart-edu/infra/scripts/mongo-ttl-indexes.sh
# 适用于「集合已存在、不重启应用」的场景；新环境由应用 auto-index-creation 自动建。
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

MONGO_CONTAINER="${MONGO_CONTAINER:-edu-mongo}"
MONGO_DB="${MONGO_DB:-edu_ai}"
# 鉴权凭据从环境变量注入，禁止硬编码（CLAUDE 9.2）。开启鉴权时设置：
#   MONGO_USER=root MONGO_PASS=*** [MONGO_AUTH_DB=admin] bash mongo-ttl-indexes.sh
MONGO_USER="${MONGO_USER:-root}"
MONGO_AUTH_DB="${MONGO_AUTH_DB:-admin}"
DIR="$(cd "$(dirname "$0")" && pwd)"

echo "==> 在容器 ${MONGO_CONTAINER} 的 ${MONGO_DB} 库配置 TTL 索引"
# 优先 mongosh（Mongo 5+），回退 mongo legacy shell（Mongo 4.x）
if docker exec "${MONGO_CONTAINER}" sh -lc 'command -v mongosh' >/dev/null 2>&1; then
  SHELL_BIN=mongosh
else
  SHELL_BIN=mongo
fi
echo "    使用 shell: ${SHELL_BIN}"

AUTH_ARGS=()
if [[ -n "${MONGO_PASS:-}" ]]; then
  AUTH_ARGS=(-u "${MONGO_USER}" -p "${MONGO_PASS}" --authenticationDatabase "${MONGO_AUTH_DB}")
fi
docker exec -i "${MONGO_CONTAINER}" "${SHELL_BIN}" "${MONGO_DB}" "${AUTH_ARGS[@]}" --quiet < "${DIR}/mongo-ttl-indexes.js"
echo "==> 完成。"
