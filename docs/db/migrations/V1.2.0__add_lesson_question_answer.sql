-- =============================================================================
-- V1.2.0  随堂答题作答记录表
-- 背景：edu-exam 已有 lesson_question（教师推送的课堂题目），但缺学生作答落库表。
--       前端 StudentAnswerPage 需要提交答案并即时获取客观题对错/解析。
-- =============================================================================

CREATE TABLE IF NOT EXISTS lesson_question_answer (
    id                 BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    lesson_question_id BIGINT   NOT NULL                COMMENT '课堂题目ID → lesson_question.id',
    lesson_id          BIGINT   NOT NULL                COMMENT '课堂ID → lesson.id',
    question_id        BIGINT   NOT NULL                COMMENT '题目ID → question.id',
    student_id         BIGINT   NOT NULL                COMMENT '学生ID → sys_user.id',
    answer_content     TEXT                             COMMENT '学生作答内容',
    is_correct         TINYINT           DEFAULT NULL   COMMENT '批改 NULL-未判定(填空/主观/投票) 0-错 1-对',
    submitted_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '作答时间',
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '创建时间',
    updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_lq_student (lesson_question_id, student_id),
    KEY idx_lesson_id (lesson_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课堂随堂答题记录（一学生一题一条，改答覆盖）';
