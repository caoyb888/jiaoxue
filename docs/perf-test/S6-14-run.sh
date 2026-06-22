#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# S6-14 AI 任务异步队列压测 —— 轻量驱动脚本
#
# 模拟「下课高峰」：COUNT 节课同时结束，并发触发 AI 任务（思维导图）入 edu.ai.tasks，
# 验证 AiTaskConsumer（concurrency=3）平滑消费、不积压，并测量全部完成耗时。
#
# 在内网机（onlyserver）上运行：
#   bash docs/perf-test/S6-14-run.sh
#
# 直连 edu-ai:8087（绕过网关鉴权，UserContext 为空不影响 MINDMAP 任务）。
# 异步结果以 lesson_report.gen_status=2(DONE) 为完成标志，轮询至全部完成或超时 5 分钟。
# 跑完自动清理压测数据（lesson_report 行 + Mongo ai_mindmap 文档）。
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

EDU_AI="${EDU_AI:-http://localhost:8087}"
LESSON_BASE="${LESSON_BASE:-90001}"
COUNT="${COUNT:-10}"
TIMEOUT_SEC="${TIMEOUT_SEC:-300}"
MYSQL="docker exec -i edu-mysql mysql -uroot -pedu_dev_2026 -N edu_db"
MONGO_CONTAINER="${MONGO_CONTAINER:-edu-mongodb}"

LAST=$(( LESSON_BASE + COUNT - 1 ))
echo "==> 触发 ${COUNT} 节课 AI 任务: lessonId ${LESSON_BASE}..${LAST}"

START_TS=$(date +%s)

# 1) 并发触发（模拟同时下课）
pids=()
tmp=$(mktemp -d)
for ((i=0; i<COUNT; i++)); do
  lid=$(( LESSON_BASE + i ))
  (
    code_time=$(curl -s -m 10 -o /dev/null -w '%{http_code} %{time_total}' \
      -X POST "${EDU_AI}/api/v1/ai/mindmap/${lid}/regenerate")
    echo "${lid} ${code_time}" > "${tmp}/${lid}"
  ) &
  pids+=("$!")
done
for p in "${pids[@]}"; do wait "$p"; done

echo "==> 入队结果 (lessonId http_code time_total_s):"
cat "${tmp}"/* | sort
ENQUEUE_DONE_TS=$(date +%s)
echo "==> 全部入队耗时: $(( ENQUEUE_DONE_TS - START_TS ))s"

# 2) 轮询异步落库（gen_status=2 表示 DONE）
echo "==> 轮询 lesson_report.gen_status，等待全部 DONE（超时 ${TIMEOUT_SEC}s）..."
done_count=0
while :; do
  done_count=$(${MYSQL} -e \
    "SELECT COUNT(*) FROM lesson_report WHERE lesson_id BETWEEN ${LESSON_BASE} AND ${LAST} AND gen_status=2;" \
    2>/dev/null | tr -d '[:space:]')
  now=$(date +%s); elapsed=$(( now - START_TS ))
  echo "    [$(printf '%3ds' "${elapsed}")] DONE=${done_count}/${COUNT}"
  [[ "${done_count}" == "${COUNT}" ]] && break
  if (( elapsed >= TIMEOUT_SEC )); then
    echo "!!! 超时：仅 ${done_count}/${COUNT} 完成（可能积压或服务未部署最新 jar）"
    break
  fi
  sleep 5
done

END_TS=$(date +%s)
echo "─────────────────────────────────────────────"
echo "结果汇总：完成 ${done_count}/${COUNT} ，总耗时 $(( END_TS - START_TS ))s（验收阈值 ≤300s）"
echo "─────────────────────────────────────────────"

# 3) 清理压测数据
echo "==> 清理压测数据 (lesson_report + Mongo ai_mindmap)"
${MYSQL} -e "DELETE FROM lesson_report WHERE lesson_id BETWEEN ${LESSON_BASE} AND ${LAST};" 2>/dev/null || true
docker exec -i "${MONGO_CONTAINER}" mongosh edu_ai --quiet --eval \
  "db.ai_mindmap.deleteMany({lessonId:{\$gte:${LESSON_BASE},\$lte:${LAST}}})" 2>/dev/null || true
rm -rf "${tmp}"
echo "==> 完成。"
