-- S7-06 教学预警事件表（edu_db）
-- 由 edu-stat 的 XXL-Job teachingWarnCheck 写入；三类预警：
--   LOW_ATTEND        低考勤（课堂去重签到学生数低于阈值）
--   ZERO_ACTIVE       零活跃（有签到但无弹幕/提问/加分/翻页等互动）
--   FREQUENT_ABSENCE  频繁缺席（学生近 N 天出勤课次占比过低）
-- 去重键 uk_warn_dedupe 保证同一对象同一天同类预警仅一行（重复触发只刷新指标，不重置处理状态）

CREATE TABLE IF NOT EXISTS warn_event
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    warn_type       VARCHAR(32)  NOT NULL COMMENT '预警类型 LOW_ATTEND/ZERO_ACTIVE/FREQUENT_ABSENCE',
    target_type     VARCHAR(16)  NOT NULL COMMENT '预警对象类型 LESSON/STUDENT',
    target_id       BIGINT       NOT NULL COMMENT '对象ID（lesson_id 或 student_id）',
    lesson_id       BIGINT       DEFAULT NULL COMMENT '关联课堂ID',
    class_id        BIGINT       DEFAULT NULL COMMENT '关联班级ID',
    dept_id         BIGINT       DEFAULT NULL COMMENT '关联院系ID',
    teacher_id      BIGINT       DEFAULT NULL COMMENT '关联教师ID',
    stat_date       DATE         NOT NULL COMMENT '统计日期',
    metric_value    INT          NOT NULL DEFAULT 0 COMMENT '触发指标实际值',
    threshold_value INT          NOT NULL DEFAULT 0 COMMENT '阈值',
    detail          VARCHAR(512) DEFAULT NULL COMMENT '预警详情（人类可读）',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '处理状态 0未处理/1已处理/2忽略',
    handled_by      BIGINT       DEFAULT NULL COMMENT '处理人ID',
    handled_at      DATETIME     DEFAULT NULL COMMENT '处理时间',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_warn_dedupe (warn_type, target_type, target_id, stat_date),
    KEY idx_status (status),
    KEY idx_stat_date (stat_date),
    KEY idx_dept (dept_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='教学预警事件';
