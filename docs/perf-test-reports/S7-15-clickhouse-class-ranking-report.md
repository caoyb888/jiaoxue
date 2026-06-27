# S7-15 ClickHouse 性能验证报告 — 近7日班级排行

> 验收标准（开发计划 Sprint-Plan S7-15）：`lesson_stat_daily` 近7日班级排行查询 **≤ 3s（200万行测试数据）**。
> 结论：**通过**。实测稳定 **4–7 ms**，较目标快约 **400–600 倍**。

## 1. 测试环境

| 项 | 值 |
|----|----|
| 部署 | onlyserver（100.84.68.115），Docker 容器 `edu-clickhouse` |
| ClickHouse 版本 | 22.8.21.38 |
| 库 / 表 | `edu_stat_db.lesson_stat_daily` |
| 表引擎 | `ReplacingMergeTree(updated_at)`，`PARTITION BY toYYYYMM(stat_date)`，`ORDER BY (stat_date, lesson_id)` |
| 测试日期 | 2026-06-27 |

## 2. 测试数据

造数脚本：[`docs/perf-test/S7-15-clickhouse-class-ranking.sql`](../perf-test/S7-15-clickhouse-class-ranking.sql)（`INSERT … SELECT … FROM numbers(2000000)`）。

| 指标 | 值 |
|------|----|
| 总行数 | **2,000,000** |
| 班级数（class_id） | 2,000 |
| 院系数（dept_id） | 20 |
| 日期跨度 | 2025-12-30 ~ 2026-06-27（180 天） |
| 分区数 | 7（按月：202512~202606） |
| 近7日窗口实际行数 | 77,784 |

分区行数分布：

| 分区 | 行数 |
|------|------|
| 202512 | 22,222 |
| 202601 | 344,441 |
| 202602 | 311,108 |
| 202603 | 344,441 |
| 202604 | 333,330 |
| 202605 | 344,441 |
| 202606 | 300,017 |

## 3. 待测查询（近7日班级排行）

```sql
SELECT class_id,
       sum(active_student_cnt) AS total_active,
       sum(attend_count)       AS total_attend,
       round(avg(avg_score), 2) AS avg_score,
       count()                 AS lesson_cnt
FROM edu_stat_db.lesson_stat_daily
WHERE stat_date >= today() - 6 AND stat_date <= today()   -- 分区键过滤（CLAUDE.md §7.4）
GROUP BY class_id
ORDER BY total_active DESC
LIMIT 50;
```

## 4. 结果

### 4.1 端到端耗时（`clickhouse-client --time`，连续 3 次）

| 次数 | 耗时 |
|------|------|
| run 1 | 0.006 s |
| run 2 | 0.007 s |
| run 3 | 0.005 s |

### 4.2 执行画像（`system.query_log`）

| query_duration_ms | read_rows | read_bytes | memory_usage |
|-------------------|-----------|------------|--------------|
| 4 – 6 ms | 87,025 | 1.83 MiB | ~445 KiB |

**关键观察：分区键过滤生效。** 查询仅读取 87,025 行（命中近7日所在分区的相关 granule），而非全表 200 万行——读取数据量 1.83 MiB，约为全表的 **1/12**。

### 4.3 对照：去掉分区键过滤（全表聚合 200 万行）

| 场景 | 耗时 | read_rows | read_bytes |
|------|------|-----------|------------|
| 带 `stat_date` 分区过滤（近7日） | ~5 ms | 87,025 | 1.83 MiB |
| 无分区过滤（全表） | ~13 ms | 2,000,000 | 22.89 MiB |

> 即便全表扫描 200 万行也仅 13 ms（ClickHouse 列存聚合极快）；但分区键过滤把 I/O 降至 1/12，在数据量增长（多学期累积）与高并发大屏轮询场景下，收益随规模放大，是 §7.4「WHERE 必带分区键」约束的实证价值。

### 4.4 排行结果抽样（前 5）

| class_id | total_active | total_attend | lesson_cnt |
|----------|--------------|--------------|------------|
| 1820 | 3769 | 3080 | 111 |
| 1065 | 3766 | 3126 | 111 |
| 263 | 3752 | 3167 | 111 |
| 1406 | 3745 | 3662 | 111 |
| 421 | 3744 | 3530 | 111 |

## 5. 结论与建议

- ✅ **验收通过**：近7日班级排行查询 ~5 ms ≪ 3 s 目标，200 万行数据下余量充足。
- ✅ 分区键（`stat_date`）过滤生效，命中分区裁剪，读取行数与字节数显著下降。
- 建议：
  - 生产环境维持「查询 WHERE 必带 `stat_date`」约束（CLAUDE.md §7.4），尤其大屏 10s 轮询与历史报表。
  - 若后续 `lesson_stat_daily` 由「明细→日聚合」任务真实写入，建议定期 `OPTIMIZE TABLE … FINAL` 收敛 ReplacingMergeTree 重复版本，避免 part 膨胀影响扫描。
  - 本次为合成测试数据，验证后可 `TRUNCATE TABLE edu_stat_db.lesson_stat_daily` 清理（脚本末尾已附）。

---
*报告对应：Sprint 7 / S7-15 ｜ 生成日期：2026-06-27 ｜ 环境：onlyserver edu-clickhouse 22.8*
