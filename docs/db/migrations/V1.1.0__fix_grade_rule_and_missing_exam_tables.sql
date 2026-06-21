-- =============================================================================
-- V1.1.0  修复 schema 与实体代际漂移
-- 背景：V1.0.0 中 grade_rule 为旧设计（权重摊列 attend_weight/quiz_weight…），
--      而实体 GradeRule 已演进为「一类一行」(grade_type + weight)，且
--      lesson_question、exam_answer_attachment 两张表在 V1.0.0 中遗漏，
--      导致 grade 模块、课堂发题、主观题附件功能查询直接报错/不可用。
-- 说明：grade_rule 旧列在代码中已无任何引用，且无有效数据，故重建；
--      其余两张为新增表。本迁移可重复执行（IF EXISTS / IF NOT EXISTS）。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. grade_rule —— 重建以对齐 GradeRule 实体（一类一行，逻辑删除）
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS grade_rule;
CREATE TABLE grade_rule (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    class_id    BIGINT       NOT NULL                COMMENT '教学班ID → class_room.id',
    teacher_id  BIGINT       NOT NULL                COMMENT '创建教师ID → sys_user.id',
    rule_name   VARCHAR(100) NOT NULL                COMMENT '规则名称',
    grade_type  TINYINT      NOT NULL                COMMENT '成绩类型 1-期末 2-平时 3-实验 4-项目 5-出勤 6-其他',
    weight      DECIMAL(5,2) NOT NULL                COMMENT '权重百分比（同一 class_id 下合计=100，应用层校验）',
    description VARCHAR(500)          DEFAULT NULL    COMMENT '说明',
    is_deleted  TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除 0-未删 1-已删',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_class_id (class_id),
    KEY idx_teacher_id (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='成绩评分规则表（一类一行，同班各类权重合计100%）';

-- ---------------------------------------------------------------------------
-- 2. lesson_question —— 课堂题目发布记录（V1.0.0 遗漏）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lesson_question (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    lesson_id   BIGINT   NOT NULL                COMMENT '课堂ID → lesson.id',
    question_id BIGINT   NOT NULL                COMMENT '题目ID → question.id',
    teacher_id  BIGINT   NOT NULL                COMMENT '发布教师ID → sys_user.id',
    status      TINYINT  NOT NULL DEFAULT 0      COMMENT '状态 0-进行中 1-已关闭',
    opened_at   DATETIME          DEFAULT NULL   COMMENT '发布时间',
    closed_at   DATETIME          DEFAULT NULL   COMMENT '关闭时间',
    is_deleted  TINYINT  NOT NULL DEFAULT 0      COMMENT '逻辑删除 0-未删 1-已删',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '创建时间',
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_lesson_id (lesson_id),
    KEY idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课堂题目发布记录（同一课堂同时仅一题 open，业务层保证）';

-- ---------------------------------------------------------------------------
-- 3. exam_answer_attachment —— 主观题图片附件关联表（V1.0.0 遗漏）
--    注意：与已存在的 exam_attachment 是不同设计，不可混用。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS exam_answer_attachment (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    student_answer_id BIGINT       NOT NULL                COMMENT '学生作答ID → student_answer.id',
    publish_id        BIGINT       NOT NULL                COMMENT '考试发布ID → exam_publish.id',
    student_id        BIGINT       NOT NULL                COMMENT '学生ID → sys_user.id',
    question_id       BIGINT       NOT NULL                COMMENT '题目ID → question.id',
    file_key          VARCHAR(500) NOT NULL                COMMENT 'MinIO 文件路径',
    sort_order        INT          NOT NULL DEFAULT 0      COMMENT '同题多图排序',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_student_answer_id (student_answer_id),
    KEY idx_publish_student (publish_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='主观题图片附件关联表';
