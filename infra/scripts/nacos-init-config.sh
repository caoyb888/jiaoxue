#!/usr/bin/env bash
# Nacos 开发配置中心初始化脚本
# 用法：./nacos-init-config.sh [nacos-host] [nacos-port]
# 默认：100.84.68.115:18848

set -e

NACOS_HOST="${1:-100.84.68.115}"
NACOS_PORT="${2:-18848}"
NACOS_ADDR="http://${NACOS_HOST}:${NACOS_PORT}"
NAMESPACE="dev"

publish() {
  local dataId="$1"
  local group="${2:-DEFAULT_GROUP}"
  local content="$3"
  curl -sf -X POST "${NACOS_ADDR}/nacos/v1/cs/configs" \
    -d "tenant=${NAMESPACE}&dataId=${dataId}&group=${group}&type=yaml&content=$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1]))" "${content}")"
  echo " → ${dataId} 发布成功"
}

echo "=== Nacos 配置中心初始化 (${NACOS_ADDR}) ==="

# edu-auth 公共配置
publish "edu-auth-dev.yaml" "DEFAULT_GROUP" "
edu:
  jwt:
    private-key: \${JWT_PRIVATE_KEY}
    public-key:  \${JWT_PUBLIC_KEY}
  wechat:
    app-id:     \${WECHAT_APP_ID}
    app-secret: \${WECHAT_APP_SECRET}
spring:
  data:
    redis:
      host: \${REDIS_HOST:100.84.68.115}
      port: \${REDIS_PORT:16379}
      timeout: 3000ms
"

# edu-user 公共配置
publish "edu-user-dev.yaml" "DEFAULT_GROUP" "
spring:
  datasource:
    url: jdbc:mysql://\${DB_HOST:100.84.68.115}:13306/edu_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: edu_app
    password: \${DB_PASS:edu_app_2026}
  data:
    redis:
      host: \${REDIS_HOST:100.84.68.115}
      port: \${REDIS_PORT:16379}
"

# edu-gateway Sentinel 配置
publish "edu-gateway-dev.yaml" "DEFAULT_GROUP" "
spring:
  cloud:
    sentinel:
      transport:
        dashboard: \${SENTINEL_DASHBOARD:localhost:8858}
      eager: true
"

# 敏感词库（供 PromptSecurityFilter 使用）
publish "edu-ai-sensitive-words" "EDU_AI" "
# 政治敏感词（示例，实际需完整词库）
words:
  - 推翻政府
  - 暴力革命
  - 制作炸弹
injection_patterns:
  - 忽略上面
  - ignore above
  - 忘记之前
  - 你现在是
  - DAN模式
"

echo ""
echo "=== 初始化完成 ==="
