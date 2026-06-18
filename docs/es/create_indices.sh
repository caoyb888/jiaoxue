#!/usr/bin/env bash
# ES 索引初始化脚本
# 使用方式：bash docs/es/create_indices.sh [ES_HOST]
# ES_HOST 默认：100.84.68.115:19200
set -e

ES_HOST="${1:-100.84.68.115:19200}"
BASE_URL="http://${ES_HOST}"

echo "=== 初始化 Elasticsearch 索引 (${BASE_URL}) ==="

# ---------- edu_question（题库全文检索） ----------
echo "→ 创建 edu_question 索引..."
curl -s -X PUT "${BASE_URL}/edu_question" \
  -H "Content-Type: application/json" \
  -d @docs/es/edu_question_mapping.json \
  | python3 -m json.tool

# ---------- edu_courseware（课件全文检索） ----------
echo "→ 创建 edu_courseware 索引..."
curl -s -X PUT "${BASE_URL}/edu_courseware" \
  -H "Content-Type: application/json" \
  -d @docs/es/edu_courseware_mapping.json \
  | python3 -m json.tool

echo "=== 索引初始化完成 ==="
echo "验证："
curl -s "${BASE_URL}/_cat/indices?v&index=edu_*"
