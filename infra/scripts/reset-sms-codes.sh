#!/usr/bin/env bash
# ============================================================
# 重置开发测试短信验证码（内网机执行）
#
# dev 环境验证码不真发短信，写在 Redis：键 sms:code:{手机号}。
# 登录成功会消费（删除）该验证码，反复测试时用本脚本快速重置。
#
# 用法：
#   bash ~/smart-edu/infra/scripts/reset-sms-codes.sh                 # 所有测试账号，码=123456
#   bash ~/smart-edu/infra/scripts/reset-sms-codes.sh 888888          # 所有测试账号，码=888888
#   bash ~/smart-edu/infra/scripts/reset-sms-codes.sh 123456 13800000001  # 仅该手机号
#
# 环境变量：
#   TTL   验证码有效期秒数（默认 86400 = 24h）
#
# 注意：前端登录时请【直接输入验证码，不要点“发送验证码”】，
#       否则后端会用新的随机码覆盖本脚本写入的固定码。
# ============================================================
set -euo pipefail

CODE="${1:-123456}"
PHONE_ARG="${2:-}"
TTL="${TTL:-86400}"

REDIS_CTN="edu-redis"
MYSQL_CTN="edu-mysql"
MYSQL_PWD="edu_dev_2026"
DB="edu_db"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info() { echo -e "${GREEN}[INFO]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# 校验验证码格式（6 位数字）
[[ "$CODE" =~ ^[0-9]{6}$ ]] || err "验证码必须是 6 位数字：$CODE"

# 依赖容器检查
docker ps --format '{{.Names}}' | grep -q "^${REDIS_CTN}$" || err "${REDIS_CTN} 未运行"

# 确定要重置的手机号列表
if [[ -n "$PHONE_ARG" ]]; then
  [[ "$PHONE_ARG" =~ ^1[3-9][0-9]{9}$ ]] || err "手机号格式不正确：$PHONE_ARG"
  PHONES="$PHONE_ARG"
else
  # 从数据库动态拉取所有启用的测试账号手机号（新增账号自动包含）
  docker ps --format '{{.Names}}' | grep -q "^${MYSQL_CTN}$" || err "${MYSQL_CTN} 未运行（无法拉取账号列表）"
  PHONES=$(docker exec "$MYSQL_CTN" mysql -uroot -p"$MYSQL_PWD" "$DB" -N -e \
    "SELECT phone_cipher FROM sys_user WHERE is_deleted=0 AND status=1 AND phone_cipher REGEXP '^1[3-9][0-9]{9}\$';" 2>/dev/null)
  [[ -n "$PHONES" ]] || err "未从数据库取到任何手机号"
fi

count=0
for p in $PHONES; do
  docker exec "$REDIS_CTN" redis-cli SETEX "sms:code:${p}" "$TTL" "$CODE" >/dev/null
  count=$((count + 1))
done

info "已重置 ${count} 个验证码 = ${CODE}（有效期 ${TTL}s）"
info "前端登录：手机号 + ${CODE}（直接输入，勿点“发送验证码”）"
