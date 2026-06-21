-- ClickHouse 统计分析表初始化
-- 数据库：edu_stat_db（docker-compose 中已配置 CLICKHOUSE_DB）
-- 注意：所有查询 WHERE 子句必须包含分区键 stat_date，避免全表扫描

-- ─── 课堂事件日志（明细层，Kafka 消费者批量写入，每批≥1000条）──────────────────
CREATE TABLE IF NOT EXISTS edu_stat_db.lesson_event_log
(
    stat_date     Date,
    lesson_id     Int64,
    class_id      Int64,
    dept_id       Int64,
    teacher_id    Int64,
    event_type    LowCardinality(String),  -- ATTEND/BARRAGE/QUESTION/SCORE/SLIDE
    student_id    Int64,
    event_value   String,
    created_at    DateTime DEFAULT now()
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(stat_date)
ORDER BY (stat_date, lesson_id, event_type, student_id)
SETTINGS index_granularity = 8192;

-- ─── 每日课堂统计（聚合层，由 lesson_event_log 汇总）────────────────────────────
CREATE TABLE IF NOT EXISTS edu_stat_db.lesson_stat_daily
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
    lesson_duration    Int32  DEFAULT 0,   -- 分钟
    updated_at         DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(updated_at)
PARTITION BY toYYYYMM(stat_date)
ORDER BY (stat_date, lesson_id)
SETTINGS index_granularity = 8192;

-- ─── 院系教学统计（周/月粒度，大屏展示和预警引擎使用）──────────────────────────
CREATE TABLE IF NOT EXISTS edu_stat_db.dept_teaching_stat
(
    stat_date          Date,
    stat_period        LowCardinality(String),   -- day/week/month
    dept_id            Int64,
    dept_name          String,
    lesson_count       Int32  DEFAULT 0,
    student_count      Int32  DEFAULT 0,
    avg_attend_rate    Float32 DEFAULT 0,
    avg_active_rate    Float32 DEFAULT 0,
    zero_active_count  Int32  DEFAULT 0,         -- 零互动课堂数（预警用）
    low_attend_count   Int32  DEFAULT 0,         -- 低考勤课堂数（预警用）
    updated_at         DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(updated_at)
PARTITION BY toYYYYMM(stat_date)
ORDER BY (stat_date, stat_period, dept_id)
SETTINGS index_granularity = 8192;
