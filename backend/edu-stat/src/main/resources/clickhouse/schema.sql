-- ClickHouse 统计分析表（edu-stat 启动时由 ClickHouseSchemaInitializer 幂等执行）
-- 与 docs/db/clickhouse_schema.sql 保持一致；所有查询 WHERE 必须含分区键 stat_date
CREATE TABLE IF NOT EXISTS lesson_event_log
(
    stat_date     Date,
    lesson_id     Int64,
    class_id      Int64,
    dept_id       Int64,
    teacher_id    Int64,
    event_type    LowCardinality(String),
    student_id    Int64,
    event_value   String,
    created_at    DateTime DEFAULT now()
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(stat_date)
ORDER BY (stat_date, lesson_id, event_type, student_id)
SETTINGS index_granularity = 8192;

CREATE TABLE IF NOT EXISTS lesson_stat_daily
(
    stat_date          Date,
    lesson_id          Int64,
    class_id           Int64,
    dept_id            Int64,
    teacher_id         Int64,
    attend_count       Int32  DEFAULT 0,
    barrage_count      Int32  DEFAULT 0,
    question_count     Int32  DEFAULT 0,
    avg_score          Float32 DEFAULT 0,
    active_student_cnt Int32  DEFAULT 0,
    lesson_duration    Int32  DEFAULT 0,
    updated_at         DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(updated_at)
PARTITION BY toYYYYMM(stat_date)
ORDER BY (stat_date, lesson_id)
SETTINGS index_granularity = 8192;

CREATE TABLE IF NOT EXISTS dept_teaching_stat
(
    stat_date          Date,
    stat_period        LowCardinality(String),
    dept_id            Int64,
    dept_name          String,
    lesson_count       Int32  DEFAULT 0,
    student_count      Int32  DEFAULT 0,
    avg_attend_rate    Float32 DEFAULT 0,
    avg_active_rate    Float32 DEFAULT 0,
    zero_active_count  Int32  DEFAULT 0,
    low_attend_count   Int32  DEFAULT 0,
    updated_at         DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(updated_at)
PARTITION BY toYYYYMM(stat_date)
ORDER BY (stat_date, stat_period, dept_id)
SETTINGS index_granularity = 8192;
