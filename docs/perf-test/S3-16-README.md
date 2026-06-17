# S3-16 签到接口压测说明

## 压测目标（Sprint 3 验收标准）

| 指标 | 目标值 |
|------|--------|
| 并发学生数 | 3000 人 |
| 总时长 | 2 分钟内完成 |
| TPS | ≥ 1500 请求/秒 |
| P99 响应时间 | ≤ 200ms |
| MySQL 错误 | 0（无连接池耗尽） |
| 重复签到率 | 0（BloomFilter 去重验证） |

## 执行步骤

```bash
# 1. 确保内网机服务已启动
ssh onlyserver 'docker compose -f ~/smart-edu/infra/docker-compose.dev.yml up -d'
ssh onlyserver 'java -jar ~/apps/edu-interaction.jar &'

# 2. 初始化测试数据（创建 3000 个测试学生账号）
mysql -h 100.84.68.115 -P 13306 -u root -pedu_dev_2026 edu_db << SQL
INSERT INTO sys_user (username, nickname, phone, role, dept_id, status)
SELECT 
  CONCAT('perf_student_', n),
  CONCAT('压测学生', n),
  CONCAT('139', LPAD(n, 8, '0')),
  'STUDENT',
  1,
  1
FROM (
  SELECT a.n + b.n * 10 + c.n * 100 + d.n * 1000 + 1 AS n
  FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a
  CROSS JOIN (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
               UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b
  CROSS JOIN (SELECT 0 n UNION SELECT 1 UNION SELECT 2) c
  CROSS JOIN (SELECT 0 n UNION SELECT 1 UNION SELECT 2) d
) numbers WHERE n <= 3000;
SQL

# 3. 创建课堂并获取签到码
curl -X POST http://100.84.68.115:18080/api/v1/interaction/lesson/1/attend/code \
  -H "X-User-Id: 1" | python3 -m json.tool

# 4. 运行 JMeter 压测（需本机安装 JMeter 5.6+）
jmeter -n \
  -t docs/perf-test/S3-16-attend-stress-test.jmx \
  -JLESSON_ID=1 \
  -JATTEND_CODE=<从上步获取的code> \
  -l target/perf-reports/S3-16-results.jtl \
  -e -o target/perf-reports/html-report/

# 5. 查看报告
open target/perf-reports/html-report/index.html

# 6. 验收检查
## 6.1 检查签到记录数（应等于实际不重复签到人数）
mysql -h 100.84.68.115 -P 13306 -u root -pedu_dev_2026 edu_db \
  -e "SELECT COUNT(*) as total, COUNT(DISTINCT student_id) as unique_students FROM attendance WHERE lesson_id = 1"

## 6.2 检查 MySQL 连接池（无耗尽错误）
ssh onlyserver 'docker logs edu-interaction 2>&1 | grep -i "connection\|pool\|timeout" | tail -20'

## 6.3 检查 Redis 签到计数与 DB 记录是否一致
redis-cli -h 100.84.68.115 -p 16379 GET "attend:count:1"
```

## 预期结果

```
签到接口 TPS：≥ 1500/s   ← 验收通过标准
P99 响应时间：≤ 200ms    ← 验收通过标准
MySQL 错误：0            ← 必须为 0
重复记录数：0            ← BloomFilter 去重效果
```

## 失败处置

若 TPS 未达标：
1. 检查 Redis List 队列落库速度（`AttendanceFlushScheduler` 每 500ms 触发一次）
2. 适当增加 HikariCP 连接池大小（`maximum-pool-size: 30`）
3. 检查 Redisson BloomFilter 性能（可改为 Guava BloomFilter 内存版减少网络 RTT）
