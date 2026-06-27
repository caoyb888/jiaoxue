-- S7-15 ClickHouse 性能验证：lesson_stat_daily 近7日班级排行查询 ≤ 3s（200万行）
-- 运行环境：onlyserver 容器 edu-clickhouse（ClickHouse 22.8），库 edu_stat_db
-- 运行方式：
--   ssh onlyserver 'docker exec -i edu-clickhouse clickhouse-client --user default \
--     --password edu_ch_2026 --multiquery < /dev/stdin' < docs/perf-test/S7-15-clickhouse-class-ranking.sql
-- 或逐段用 clickhouse-client --query 执行（见 docs/perf-test-reports/S7-15-clickhouse-perf-report.md）

-- ── 1) 造数：200 万行，2000 个班级，跨 180 天（7 个月分区）─────────────────────
-- lesson_id 唯一（ReplacingMergeTree 按 (stat_date, lesson_id) 去重，不会塌行）
INSERT INTO edu_stat_db.lesson_stat_daily
SELECT
    toDate('2026-06-27') - toIntervalDay(number % 180) AS stat_date,
    number                                             AS lesson_id,
    number % 2000                                      AS class_id,
    number % 20                                        AS dept_id,
    number % 500                                       AS teacher_id,
    toInt32(rand(1) % 60)                              AS attend_count,
    toInt32(rand(2) % 100)                             AS barrage_count,
    toInt32(rand(3) % 20)                              AS question_count,
    toFloat32(60 + (rand(4) % 40))                     AS avg_score,
    toInt32(rand(5) % 60)                              AS active_student_cnt,
    45                                                 AS lesson_duration,
    now()                                              AS updated_at
FROM numbers(2000000);

-- ── 2) 待测查询：近7日班级活跃排行（WHERE 必带 stat_date 分区键，CLAUDE.md §7.4）──
SELECT class_id,
       sum(active_student_cnt) AS total_active,
       sum(attend_count)       AS total_attend,
       round(avg(avg_score), 2) AS avg_score,
       count()                 AS lesson_cnt
FROM edu_stat_db.lesson_stat_daily
WHERE stat_date >= today() - 6 AND stat_date <= today()
GROUP BY class_id
ORDER BY total_active DESC
LIMIT 50;

-- ── 3) 执行画像（耗时/扫描行数/读取字节）──────────────────────────────────────
SYSTEM FLUSH LOGS;
SELECT query_duration_ms AS ms,
       read_rows,
       formatReadableSize(read_bytes)   AS read_bytes,
       formatReadableSize(memory_usage) AS mem
FROM system.query_log
WHERE query LIKE '%total_active%' AND type = 'QueryFinish'
ORDER BY event_time DESC
LIMIT 5;

-- ── 清理（按需）──────────────────────────────────────────────────────────────
-- TRUNCATE TABLE edu_stat_db.lesson_stat_daily;
