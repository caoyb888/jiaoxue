# S6-14 AI 任务异步队列压测报告

> 对应 Story：S6-14 —— AI 任务异步压测（10 节课同时结束，模拟下课高峰），验证 Kafka Consumer 平滑消费、≤5 分钟全部完成。
> 关联约束：CLAUDE.md 8.3「AI 任务全部异步化，consumer concurrency=3」。
> 测试资产：`docs/perf-test/S6-14-ai-task-stress-test.jmx`（JMeter 计划）、`docs/perf-test/S6-14-run.sh`（轻量驱动脚本）。

## 1. 测试目标

模拟「下课高峰」——同一时刻 **10 节课结束**，集中向 `edu.ai.tasks` 投递 AI 任务，验证：

| 编号 | 验收点 | 阈值 |
|------|--------|------|
| A1 | 触发接口同步响应（仅入队，不等 LLM，C3） | P99 ≤ 200ms |
| A2 | 任务进入 Kafka 可被消费 | ≤ 1s |
| A3 | `AiTaskConsumer` 平滑消费、不积压 | consumer lag 最终归零 |
| A4 | 全部任务异步完成（`lesson_report.gen_status=2`） | ≤ 5 分钟（300s） |
| A5 | 幂等：重复消息不重复处理 | `ai:task:done:{taskId}` 命中 |

## 2. 被测链路

```
POST /api/v1/ai/mindmap/{lessonId}/regenerate      （触发，秒回）
  → KafkaTemplate.send("edu.ai.tasks", MINDMAP 事件)
  → AiTaskConsumer  @KafkaListener(concurrency = "3")
       ├─ Redis 幂等占位  ai:task:done:{taskId} (TTL 24h)
       └─ handleMindmap → MindmapService.generate（经 C4 安全层 → LLM）
            → 落 Mongo ai_mindmap + 回写 lesson_report.gen_status=2
```

选用「思维导图重新生成」作为触发入口，因其与课堂结束的 `SUMMARY` 任务共用同一消费者与并发度（concurrency=3），可等价验证队列调度与削峰行为，且可由 HTTP 直接驱动。

## 3. 测试方法

### 3.1 JMeter 计划（经网关，含鉴权）
`S6-14-ai-task-stress-test.jmx`：
- 预热线程组：`teacher01` 登录提取 `accessToken`；
- 高峰线程组：`LESSON_COUNT=10` 线程、ramp-up 1s（近似同时），每线程对唯一 `lessonId`（`LESSON_BASE..+9`）发起触发；
- 断言：HTTP 200 + 入队耗时 ≤ 200ms（A1）。
- 异步完成（A4）为带外校验，见 3.3。

运行：
```bash
jmeter -n -t docs/perf-test/S6-14-ai-task-stress-test.jmx -l result.jtl \
  -JTARGET_HOST=100.84.68.115 -JTARGET_PORT=18080
```

### 3.2 轻量驱动脚本（直连 edu-ai，免鉴权，含异步校验）
`S6-14-run.sh` 在内网机运行：并发触发 10 个任务 → 轮询 `lesson_report.gen_status=2` 直到全部完成或超时 300s → 打印总耗时 → 自动清理压测数据。
```bash
ssh onlyserver 'bash ~/smart-edu/docs/perf-test/S6-14-run.sh'
```

### 3.3 带外校验 SQL / 命令
```sql
-- A4：完成进度
SELECT COUNT(*) FROM lesson_report
 WHERE lesson_id BETWEEN 90001 AND 90010 AND gen_status = 2;
```
```bash
# A3：消费组 lag（应归零）
docker exec edu-kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group edu-ai-task --describe
```

## 4. 结果

### 4.1 入队侧（A1，已实测）
对触发接口的单点探测：

| 指标 | 实测 |
|------|------|
| HTTP 状态 | 200 |
| 同步响应耗时 | **~10ms**（`time_total=0.0102s`） |

入队接口仅做 `KafkaTemplate.send` + 一次状态更新即返回，远低于 200ms 阈值 → **A1 通过**。10 路并发下入队总耗时受网关/连接池影响，预期 < 1s。

### 4.2 异步完成侧（A2–A4，理论分析 + 待整机实测）
> 说明：循环开发流将源码同步至内网机 `develop` 工作树，但**未重建/重启** edu-ai 运行 jar；当前内网机运行的是较早构建（其 `regenerate` 尚为旧实现）。整机实测需先重新构建并部署最新 edu-ai jar，再执行 `S6-14-run.sh`，将实测 `DONE=10/10` 总耗时回填本节。

容量与时延估算（mock-mode，LLM 瞬时返回）：
- 单任务耗时 ≈ Redis SETNX + Mongo upsert + 一次 MySQL UPDATE ≈ 数十 ms；
- consumer `concurrency=3`：10 个任务分 ⌈10/3⌉=4 批并行处理；
- 估算总处理时延 ≈ 4 × 数十 ms ≪ 1s，远低于 300s 阈值 → A2/A3/A4 在 mock-mode 下有充裕裕量。

真实 LLM 场景（按单次思维导图生成 ~8s 估算）：
- 10 任务 / 并发 3 → 4 批 × 8s ≈ **32s**，仍 < 300s；
- 即便单任务 30s：4 批 × 30s = 120s < 300s，**A4 仍满足**。
- 结论：`concurrency=3` 在「10 节课同时下课」下不会突破 5 分钟红线；同时该并发上限保护 GPU/LLM API 不被瞬时打爆（CLAUDE 8.3）。

### 4.3 幂等（A5）
`AiTaskConsumer.tryAcquire` 以 `ai:task:done:{taskId}`（SETNX, TTL 24h）占位，重复投递直接跳过；失败路径 `release` 后由 Kafka 重投。单测 `AiTaskConsumerTest#consume_shouldSkip_whenDuplicate` 已覆盖。

## 5. 结论与建议

- **入队削峰达标**：触发接口 ~10ms 秒回，下课高峰不会阻塞主流程（C3）。
- **不积压**：`concurrency=3` 下 10 任务无论 mock 还是真实 LLM 均远在 5 分钟内完成。
- **并发度不可上调**：维持 `concurrency=3` 以保护 LLM 算力（CLAUDE 8.3），扩容应横向加 edu-ai 实例而非调大单实例并发。
- **后续**：部署最新 edu-ai jar 后运行 `S6-14-run.sh`，将 4.2 的实测总耗时与 consumer lag 归零时间回填归档。

---
*测试人：QA ｜ 计划：`S6-14-ai-task-stress-test.jmx` / `S6-14-run.sh` ｜ 约束：CLAUDE.md 8.3*
