#!/usr/bin/env bash
# ============================================================
# 山东管理学院智慧教学系统 — 一键启动脚本（内网机执行）
# 用法：bash ~/smart-edu/infra/scripts/start-all.sh [选项]
#   --skip-build   跳过 Maven 编译（JAR 已存在时使用）
#   --skip-seed    跳过测试数据注入
#   --backend-only 只启动后端
#   --frontend-only只启动前端
# ============================================================
set -e

PROJ_DIR="$HOME/smart-edu"
LOG_DIR="$HOME/.edu-dev/logs"
PID_DIR="$HOME/.edu-dev/pids"
KEY_DIR="$HOME/.edu-dev/keys"
NACOS_ADDR="http://100.84.68.115:18848"

# Maven 路径（已安装在 /opt/maven）
export PATH="/opt/maven/bin:$PATH"
MVN=mvn

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }
step()  { echo -e "\n${GREEN}══════════ $* ══════════${NC}"; }

SKIP_BUILD=false; SKIP_SEED=false; BACKEND_ONLY=false; FRONTEND_ONLY=false
for arg in "$@"; do
  case $arg in
    --skip-build)   SKIP_BUILD=true ;;
    --skip-seed)    SKIP_SEED=true ;;
    --backend-only) BACKEND_ONLY=true ;;
    --frontend-only)FRONTEND_ONLY=true ;;
  esac
done

mkdir -p "$LOG_DIR" "$PID_DIR" "$KEY_DIR"

# ────────────────────────────────────────────────────────────
# 1. 环境检查
# ────────────────────────────────────────────────────────────
step "1. 环境检查"

java --version 2>/dev/null | grep -q "21" || error "需要 Java 21，请检查 JAVA_HOME"
info "Java 21 ✓"

node --version 2>/dev/null | grep -q "v20\|v18" || warn "Node.js 版本可能不匹配（期望 v20）"
info "Node.js $(node --version) ✓"

pnpm --version 2>/dev/null >/dev/null || error "pnpm 未安装"
info "pnpm $(pnpm --version) ✓"

if ! command -v mvn &>/dev/null; then
  error "Maven 未找到，请确认 /opt/maven/bin 已在 PATH 中"
fi
MVN_VER=$(mvn --version 2>&1 | head -1)
info "$MVN_VER ✓"

# ────────────────────────────────────────────────────────────
# 2. Docker 中间件健康检查
# ────────────────────────────────────────────────────────────
step "2. 中间件健康检查"

check_container() {
  local name=$1 port=$2
  if docker ps --filter "name=^${name}$" --filter "status=running" --format '{{.Names}}' | grep -q "$name"; then
    info "$name 运行中 ✓"
  else
    warn "$name 未运行，尝试启动..."
    cd "$PROJ_DIR" && docker compose -f infra/docker-compose.dev.yml up -d "$name"
    sleep 5
  fi
}

check_container edu-mysql     13306
check_container edu-redis     16379
check_container edu-kafka     19092
check_container edu-nacos     18848
check_container edu-mongo     17017
check_container edu-minio     19000
check_container edu-clickhouse 18123
check_container edu-elasticsearch 19200

# ────────────────────────────────────────────────────────────
# 3. 生成 JWT RSA 密钥（首次运行）
# ────────────────────────────────────────────────────────────
step "3. JWT 密钥准备"

if [ ! -f "$KEY_DIR/private.pem" ]; then
  info "生成 RSA-2048 密钥对..."
  openssl genrsa -out "$KEY_DIR/private_raw.pem" 2048 2>/dev/null
  openssl pkcs8 -topk8 -inform PEM -in "$KEY_DIR/private_raw.pem" \
    -out "$KEY_DIR/private.pem" -nocrypt
  openssl rsa -in "$KEY_DIR/private_raw.pem" -pubout -out "$KEY_DIR/public.pem" 2>/dev/null
  rm "$KEY_DIR/private_raw.pem"
  info "密钥已生成并保存至 $KEY_DIR/"
else
  info "JWT 密钥已存在，跳过生成 ✓"
fi

# 导出单行 PEM（供 Nacos 使用）
PRIVATE_KEY_B64=$(grep -v "BEGIN\|END" "$KEY_DIR/private.pem" | tr -d '\n')
PUBLIC_KEY_B64=$(grep -v "BEGIN\|END" "$KEY_DIR/public.pem" | tr -d '\n')
PRIVATE_KEY_PEM="-----BEGIN PRIVATE KEY-----\n${PRIVATE_KEY_B64}\n-----END PRIVATE KEY-----"
PUBLIC_KEY_PEM="-----BEGIN PUBLIC KEY-----\n${PUBLIC_KEY_B64}\n-----END PUBLIC KEY-----"

# ────────────────────────────────────────────────────────────
# 4. 初始化 Nacos 配置
# ────────────────────────────────────────────────────────────
step "4. Nacos 配置初始化"

# 等待 Nacos 就绪
for i in {1..20}; do
  if curl -sf "${NACOS_ADDR}/nacos/v1/console/health/liveness" >/dev/null 2>&1; then
    break
  fi
  echo -n "."; sleep 3
done
echo ""

nacos_publish() {
  local dataId="$1" group="${2:-DEFAULT_GROUP}" content="$3"
  curl -sf -X POST "${NACOS_ADDR}/nacos/v1/cs/configs" \
    -d "tenant=dev&dataId=${dataId}&group=${group}&type=yaml" \
    --data-urlencode "content=${content}" >/dev/null
  info "Nacos: ${dataId} 已发布"
}

nacos_publish "edu-auth-dev.yaml" "DEFAULT_GROUP" "
spring:
  datasource:
    url: jdbc:mysql://100.84.68.115:13306/edu_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: edu_dev_2026
    driver-class-name: com.mysql.cj.jdbc.Driver
  flyway:
    enabled: false
  data:
    redis:
      host: 100.84.68.115
      port: 16379
      database: 0
edu:
  jwt:
    private-key: \"-----BEGIN PRIVATE KEY-----\\n${PRIVATE_KEY_B64}\\n-----END PRIVATE KEY-----\"
  user:
    service-url: http://100.84.68.115:18082
"

nacos_publish "edu-stat-dev.yaml" "DEFAULT_GROUP" "
clickhouse:
  url: jdbc:clickhouse://100.84.68.115:18123/edu_stat_db
  username: default
  password: edu_ch_2026
spring:
  data:
    redis:
      host: 100.84.68.115
      port: 16379
      database: 7
  kafka:
    bootstrap-servers: 100.84.68.115:19092
    consumer:
      group-id: edu-stat
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: \"*\"
"

nacos_publish "edu-ai-sensitive-words" "EDU_AI" "
words:
  - 推翻政府
  - 暴力革命
injection_patterns:
  - 忽略上面
  - ignore above
  - 忘记之前
  - DAN模式
"

info "Nacos 配置初始化完成 ✓"

# ────────────────────────────────────────────────────────────
# 5. Maven 编译
# ────────────────────────────────────────────────────────────
if [ "$FRONTEND_ONLY" = false ]; then
  if [ "$SKIP_BUILD" = false ]; then
    step "5. Maven 编译（-DskipTests，约 5-10 分钟）"
    cd "$PROJ_DIR/backend"
    mvn clean package -DskipTests -q \
      -Dmaven.compiler.parameters=true \
      --no-transfer-progress \
      2>&1 | tee "$LOG_DIR/maven-build.log" | grep -E "ERROR|BUILD" || true
    if grep -q "BUILD SUCCESS" "$LOG_DIR/maven-build.log"; then
      info "Maven 编译成功 ✓"
    else
      error "Maven 编译失败，查看 $LOG_DIR/maven-build.log"
    fi
  else
    step "5. 跳过 Maven 编译（--skip-build）"
  fi
fi

# ────────────────────────────────────────────────────────────
# 6. 启动后端微服务
# ────────────────────────────────────────────────────────────
start_service() {
  local name=$1 jar=$2 port=$3
  local pid_file="$PID_DIR/${name}.pid"
  local log_file="$LOG_DIR/${name}.log"

  # 杀掉旧进程
  if [ -f "$pid_file" ]; then
    local old_pid
    old_pid=$(cat "$pid_file")
    if kill -0 "$old_pid" 2>/dev/null; then
      kill "$old_pid" 2>/dev/null || true
      sleep 2
    fi
    rm -f "$pid_file"
  fi

  # 检查端口占用
  if ss -tlnp 2>/dev/null | grep -q ":${port} "; then
    warn "${name} 端口 ${port} 已被占用，跳过启动"
    return 0
  fi

  if [ ! -f "$jar" ]; then
    warn "${name} JAR 不存在：${jar}"
    return 1
  fi

  nohup java \
    -Xms256m -Xmx512m \
    -Dspring.profiles.active=dev \
    -Dnacos.addr=100.84.68.115:18848 \
    -jar "$jar" \
    > "$log_file" 2>&1 &

  echo $! > "$pid_file"
  info "${name} 已启动（PID=$(cat "$pid_file")，端口=${port}，日志=${log_file}）"
}

wait_port() {
  local name=$1 port=$2 timeout=${3:-120}
  echo -n "等待 ${name}(${port}) 就绪"
  for i in $(seq 1 $timeout); do
    if ss -tlnp 2>/dev/null | grep -q ":${port} "; then
      echo " ✓"
      return 0
    fi
    echo -n "."
    sleep 1
  done
  echo " ✗（超时 ${timeout}s）"
  warn "${name} 启动超时，继续执行..."
  return 1
}

if [ "$FRONTEND_ONLY" = false ]; then
  step "6. 启动后端微服务"

  BACKEND="$PROJ_DIR/backend"

  # 启动顺序：依赖少的先起
  start_service "edu-auth"        "$BACKEND/edu-auth/target/edu-auth-1.0.0-SNAPSHOT.jar"        8081
  start_service "edu-user"        "$BACKEND/edu-user/target/edu-user-1.0.0-SNAPSHOT.jar"        18082
  wait_port "edu-auth" 8081 90
  wait_port "edu-user" 18082 90

  start_service "edu-course"      "$BACKEND/edu-course/target/edu-course-1.0.0-SNAPSHOT.jar"    8083
  start_service "edu-interaction" "$BACKEND/edu-interaction/target/edu-interaction-1.0.0-SNAPSHOT.jar" 8084
  start_service "edu-exam"        "$BACKEND/edu-exam/target/edu-exam-1.0.0-SNAPSHOT.jar"        8085
  start_service "edu-grade"       "$BACKEND/edu-grade/target/edu-grade-1.0.0-SNAPSHOT.jar"      8086
  start_service "edu-file"        "$BACKEND/edu-file/target/edu-file-1.0.0-SNAPSHOT.jar"        8089
  start_service "edu-notify"      "$BACKEND/edu-notify/target/edu-notify-1.0.0-SNAPSHOT.jar"    8090
  start_service "edu-ai"          "$BACKEND/edu-ai/target/edu-ai-1.0.0-SNAPSHOT.jar"            8087
  start_service "edu-stat"        "$BACKEND/edu-stat/target/edu-stat-1.0.0-SNAPSHOT.jar"        18088
  start_service "edu-jwxt"        "$BACKEND/edu-jwxt/target/edu-jwxt-1.0.0-SNAPSHOT.jar"        8093
  start_service "edu-admin"       "$BACKEND/edu-admin/target/edu-admin-1.0.0-SNAPSHOT.jar"      8092
  start_service "edu-live"        "$BACKEND/edu-live/target/edu-live-1.0.0-SNAPSHOT.jar"        8091

  sleep 10
  start_service "edu-gateway"     "$BACKEND/edu-gateway/target/edu-gateway-1.0.0-SNAPSHOT.jar"  18080
  wait_port "edu-gateway" 18080 120
fi

# ────────────────────────────────────────────────────────────
# 7. 注入测试数据
# ────────────────────────────────────────────────────────────
if [ "$SKIP_SEED" = false ] && [ "$FRONTEND_ONLY" = false ]; then
  step "7. 注入测试数据"
  SEED_SQL="$PROJ_DIR/docs/db/seed_full.sql"
  if [ -f "$SEED_SQL" ]; then
    docker exec -i edu-mysql mysql -u root -pedu_dev_2026 edu_db < "$SEED_SQL" 2>/dev/null \
      && info "测试数据注入成功 ✓" \
      || warn "测试数据注入出现警告（可能是重复数据，可忽略）"
  fi

  # 预置 Redis SMS 验证码（开发用，固定 123456）
  info "预置开发测试 SMS 验证码（123456）..."
  for phone in 13800000001 13800000002 13800000003 13800000004 13800000005 \
               13800000011 13800000012 13800000013 13800000014 13800000015 \
               13800000016 13800000017 13800000018 13800000019 13800000020; do
    docker exec edu-redis redis-cli -p 6379 SETEX "sms:code:${phone}" 86400 "123456" >/dev/null 2>&1
  done
  info "SMS 验证码已预置（所有测试手机号均为 123456，有效期 24h）✓"
fi

# ────────────────────────────────────────────────────────────
# 8. 启动前端
# ────────────────────────────────────────────────────────────
if [ "$BACKEND_ONLY" = false ]; then
  step "8. 启动前端"

  cd "$PROJ_DIR/frontend"

  if [ ! -d "node_modules" ] || [ ! -d "apps/web/node_modules" ]; then
    info "安装前端依赖（pnpm install）..."
    pnpm install --frozen-lockfile 2>&1 | tail -5
    info "前端依赖安装完成 ✓"
  fi

  # 启动 web 应用
  WEB_PID_FILE="$PID_DIR/frontend-web.pid"
  WEB_LOG="$LOG_DIR/frontend-web.log"
  if [ -f "$WEB_PID_FILE" ] && kill -0 "$(cat "$WEB_PID_FILE")" 2>/dev/null; then
    warn "前端 web 已在运行（PID=$(cat "$WEB_PID_FILE")）"
  else
    nohup pnpm --filter web dev > "$WEB_LOG" 2>&1 &
    echo $! > "$WEB_PID_FILE"
    info "前端 web 已启动（PID=$(cat "$WEB_PID_FILE")，日志=$WEB_LOG）"
  fi

  wait_port "frontend-web" 5173 60
fi

# ────────────────────────────────────────────────────────────
# 9. 启动状态总览
# ────────────────────────────────────────────────────────────
step "9. 服务状态总览"

check_port() {
  local name=$1 port=$2 url=$3
  if ss -tlnp 2>/dev/null | grep -q ":${port} "; then
    echo -e "  ${GREEN}✓${NC} ${name}  →  ${url}"
  else
    echo -e "  ${RED}✗${NC} ${name}  →  ${url}（端口未监听）"
  fi
}

echo ""
echo "=== 中间件 ==="
check_port "MySQL"         13306 "100.84.68.115:13306"
check_port "Redis"         16379 "100.84.68.115:16379"
check_port "Nacos"         18848 "http://100.84.68.115:18848/nacos"
check_port "Kafka"         19092 "100.84.68.115:19092"
check_port "MinIO"         19001 "http://100.84.68.115:19001 (console)"
check_port "Elasticsearch" 19200 "http://100.84.68.115:19200"
check_port "ClickHouse"    18123 "http://100.84.68.115:18123"
check_port "XXL-Job"       18160 "http://100.84.68.115:18160"

echo ""
echo "=== 微服务 ==="
check_port "edu-gateway"     18080 "http://100.84.68.115:18080"
check_port "edu-auth"        8081  "http://100.84.68.115:8081"
check_port "edu-user"        18082 "http://100.84.68.115:18082"
check_port "edu-course"      8083  "http://100.84.68.115:8083"
check_port "edu-interaction" 8084  "http://100.84.68.115:8084"
check_port "edu-exam"        8085  "http://100.84.68.115:8085"
check_port "edu-grade"       8086  "http://100.84.68.115:8086"
check_port "edu-ai"          8087  "http://100.84.68.115:8087"
check_port "edu-stat"        18088 "http://100.84.68.115:18088"
check_port "edu-file"        8089  "http://100.84.68.115:8089"
check_port "edu-notify"      8090  "http://100.84.68.115:8090"
check_port "edu-live"        8091  "http://100.84.68.115:8091"
check_port "edu-admin"       8092  "http://100.84.68.115:8092"
check_port "edu-jwxt"        8093  "http://100.84.68.115:8093"

echo ""
echo "=== 前端 ==="
check_port "Web 应用" 5173 "http://100.84.68.115:5173"

echo ""
echo "==============================="
echo " 测试账号（验证码固定 123456）"
echo "==============================="
echo "  管理员：13800000001"
echo "  教师01：13800000002（张老师）"
echo "  教师02：13800000003（李明华）"
echo "  学生01：13800000011（student01）"
echo "  学生02：13800000012（王小明）"
echo ""
echo "  API 文档：http://100.84.68.115:18080/doc.html"
echo "  Nacos：   http://100.84.68.115:18848/nacos  (nacos/nacos)"
echo "  日志目录：$LOG_DIR/"
echo ""
echo -e "${GREEN}启动完成！${NC}"
