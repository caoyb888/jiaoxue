# S5-15 交卷高并发压测说明

## 压测目标

| 指标 | 目标 |
|------|------|
| 并发学生数 | 1000 |
| 提交窗口 | 30 秒（模拟考试自动交卷打散） |
| 成功率 | ≥ 99.9% |
| P99 响应时间 | ≤ 5000ms |
| 幂等保证 | 同一学生重复提交只入队一次 |

## 三阶段设计

```
阶段0（预热）  ：教师登录 → 开始考试（status=1）
阶段1（1000线）：学生登录 + 进入考试，获取 JWT Token 并缓存
                 ramp_time=5s，5秒内全部进入
阶段2（核心）  ：1000线程同时出发，UniformRandomTimer 在 0~30s 内随机等待
                 → 模拟前端 getSubmitDelay(studentNo) 打散机制
                 含图片附件（主观题 answerContent ≈ 800B JSON）
阶段3（幂等）  ：50名学生额外重复提交，期望 code=200（不报错不重复入队）
```

## 前置准备

### 1. 生成学生账号 CSV

```bash
# 生成 1000 条测试学生账号 CSV（需先在 seed_dev.sql 中批量插入学生数据）
python3 docs/perf-test/gen-students-csv.py > docs/perf-test/students-1000.csv

# 或者直接用 MySQL 导出
mysql -h 100.84.68.115 -P 13306 -u root -pedu_dev_2026 edu_db \
  -e "SELECT id,username,'edu2026@test' FROM sys_user WHERE role='STUDENT' LIMIT 1000" \
  --batch --skip-column-names > docs/perf-test/students-1000.csv
```

CSV 格式（含标题行）：
```
studentId,username,password
10001,student00001,edu2026@test
10002,student00002,edu2026@test
...
```

### 2. 初始化压测环境

```bash
# 1. 在内网机确认 edu-exam 服务、Redis、Kafka、MySQL 已启动
ssh onlyserver 'docker ps --format "table {{.Names}}\t{{.Status}}" | grep -E "edu|redis|kafka|mysql"'

# 2. 在 MySQL 中确认 exam_publish 存在（id=1，status=0 待开始）
ssh onlyserver 'mysql -u root -pedu_dev_2026 edu_db -e "SELECT id,status,paper_id FROM exam_publish WHERE id=1"'

# 3. 创建结果输出目录
mkdir -p docs/perf-test-reports

# 4. 清理上一次测试的幂等键（避免上次残留 key 影响本次）
ssh onlyserver 'docker exec edu-redis redis-cli -p 16379 keys "exam:submit:1:*" | xargs -r docker exec edu-redis redis-cli -p 16379 del'
```

### 3. 运行压测

```bash
# 方式 A：GUI 模式（调试阶段）
jmeter -t docs/perf-test/S5-15-exam-submit-stress-test.jmx

# 方式 B：命令行模式（推荐）
jmeter -n \
  -t docs/perf-test/S5-15-exam-submit-stress-test.jmx \
  -l docs/perf-test-reports/S5-15-submit-result.jtl \
  -e -o docs/perf-test-reports/S5-15-html-report \
  -JTHREAD_COUNT=1000 \
  -JSCATTER_MAX_MS=30000 \
  -JPUBLISH_ID=1 \
  -JCSV_PATH=docs/perf-test/students-1000.csv

# 方式 C：在内网机运行（推荐，避免网络延迟影响）
scp docs/perf-test/S5-15-exam-submit-stress-test.jmx onlyserver:~/
scp docs/perf-test/students-1000.csv onlyserver:~/
ssh onlyserver 'jmeter -n -t ~/S5-15-exam-submit-stress-test.jmx \
  -l ~/S5-15-result.jtl -e -o ~/S5-15-html-report'
```

## 结果分析

### 关键指标

打开 `docs/perf-test-reports/S5-15-html-report/index.html` 查看：

1. **TPS 趋势图**：应在 0~30s 内均匀分布，峰值不超过 50 TPS（1000/30≈33 TPS 平均值）
2. **响应时间分布**：P99 ≤ 5000ms（后端为 Redis SETNX + Kafka 写入，应 <500ms）
3. **错误率**：< 0.1%

### 验证打散效果

```bash
# 查看 JTL 文件中请求时间分布（每秒请求数）
awk -F',' 'NR>1 {ts=int($1/1000); count[ts]++} END {for(t in count) print t, count[t]}' \
  docs/perf-test-reports/S5-15-submit-result.jtl | sort -n
```

期望输出：每秒约 33 个请求，不出现单秒 >200 的尖峰。

### 验证幂等

```bash
# 统计 exam_submit_queue 中的记录数（应恰好等于不重复学生数，≤1000）
ssh onlyserver 'mysql -u root -pedu_dev_2026 edu_db \
  -e "SELECT COUNT(*) as total, COUNT(DISTINCT student_id) as unique_students \
      FROM exam_submit_queue WHERE publish_id=1"'
```

### 验证 Redis 幂等键

```bash
# 查看已消费幂等键数量（应 = 成功交卷人数）
ssh onlyserver 'docker exec edu-redis redis-cli -p 16379 \
  keys "exam:submit:1:*" | wc -l'
```

## 通过标准

| 检查项 | 期望值 | 失败处理 |
|--------|--------|---------|
| HTTP 成功率 | ≥ 99.9% | 查看 errors.jtl 定位接口报错 |
| P99 响应时间 | ≤ 5000ms | 检查 Kafka/Redis 连接池配置 |
| MySQL 连接池 | 无超时日志 | `ssh onlyserver 'docker logs edu-exam \| grep -i "connection timeout"'` |
| 幂等：DB 记录数 | ≤ 1000 | 检查 Redis SETNX 命中率 |
| 打散分布 | 单秒峰值 ≤ 100 | 检查 UniformRandomTimer 配置 |
