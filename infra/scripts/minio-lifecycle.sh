#!/usr/bin/env bash
# S8-07 MinIO Bucket 生命周期规则应用脚本（OPS）。
#
# 三条规则（与 edu-file BucketInitializer 一致）：
#   edu-live-replay  replay/ 60 天转冷存储（Transition→COLD）
#   edu-exam-attach  exam/   90 天过期删除（Expiration）
#   edu-slides               365 天归档转冷（Transition→COLD）
#
# 用法（在内网机或装有 mc 的机器执行）：
#   bash infra/scripts/minio-lifecycle.sh
#
# 依赖 MinIO Client(mc)。Transition→COLD 需先配置 ILM tier（无远程冷存可跳过或改 Expiration）。
set -euo pipefail

MC=${MC:-mc}
ALIAS=${MINIO_ALIAS:-edu}
ENDPOINT=${MINIO_ENDPOINT:-http://100.84.68.115:19000}
ACCESS_KEY=${MINIO_ACCESS_KEY:-minioadmin}
SECRET_KEY=${MINIO_SECRET_KEY:-minioadmin}
RULE_DIR=${RULE_DIR:-"$(dirname "$0")/../minio/lifecycle"}

echo "[minio-lifecycle] 配置别名 $ALIAS → $ENDPOINT"
"$MC" alias set "$ALIAS" "$ENDPOINT" "$ACCESS_KEY" "$SECRET_KEY" >/dev/null

apply() {
  local bucket="$1" file="$2"
  echo "[minio-lifecycle] 应用 $bucket ← $file"
  if "$MC" ilm import "$ALIAS/$bucket" < "$file"; then
    echo "[minio-lifecycle]   ✓ $bucket 规则已应用"
  else
    echo "[minio-lifecycle]   ⚠ $bucket 应用失败（Transition 需先 'mc ilm tier add' 配冷存层）"
  fi
}

apply edu-live-replay "$RULE_DIR/edu-live-replay.json"
apply edu-exam-attach "$RULE_DIR/edu-exam-attach.json"
apply edu-slides      "$RULE_DIR/edu-slides.json"

echo "[minio-lifecycle] 当前规则："
for b in edu-live-replay edu-exam-attach edu-slides; do
  echo "--- $b ---"; "$MC" ilm ls "$ALIAS/$b" || true
done
