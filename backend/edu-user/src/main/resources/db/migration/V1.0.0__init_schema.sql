-- ============================================================
-- 山东管理学院智慧教学系统 — edu_db 全量建表 DDL
-- Flyway 版本：V1.0.0
-- 对应数据库设计文档：V2.0（2026-06-16）
-- 共 40 张业务表（edu_audit_db 见 V1.0.1）
-- ============================================================

USE edu_db;

-- ════════════════════════════════════════════════════════════
-- 用户域 sys_*（4张）
-- ════════════════════════════════════════════════════════════

CREATE TABLE sys_dept (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '院系ID',
    dept_code     VARCHAR(20)  NOT NULL                COMMENT '院系编码（教务系统对照码，唯一）',
    dept_name     VARCHAR(100) NOT NULL                COMMENT '院系/行政班名称',
    parent_id     BIGINT       NULL                    COMMENT '上级院系ID → sys_dept.id（NULL=顶级）',
    dept_type     TINYINT      NOT NULL DEFAULT 1      COMMENT '节点类型：1-学校 2-学院 3-系/专业 4-行政班',
    level         TINYINT      NOT NULL DEFAULT 1      COMMENT '层级深度：1-学校 2-学院 3-专业 4-班级',
    sort_order    INT          NOT NULL DEFAULT 0      COMMENT '同级排序权重（升序）',
    is_deleted    TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-正常 1-已删除',
    jwxt_dept_id  VARCHAR(50)  NULL                    COMMENT '教务系统院系原始ID（ETL同步对照键）',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dept_code  (dept_code),
    INDEX idx_parent_id      (parent_id),
    INDEX idx_dept_type      (dept_type),
    INDEX idx_jwxt_dept_id   (jwxt_dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='组织架构表：学校→学院→专业→行政班（自引用树形结构）';

CREATE TABLE sys_user (
    id                 BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '用户ID，系统主键',
    student_no         VARCHAR(20)  NULL                     COMMENT '学号（学生专有，由教务系统同步，唯一）',
    username           VARCHAR(50)  NOT NULL                 COMMENT '用户名（系统登录名，唯一）',
    real_name          VARCHAR(50)  NOT NULL                 COMMENT '真实姓名',
    phone_cipher       VARCHAR(255) NULL                     COMMENT '手机号 AES-256 密文（禁止存明文；Redis维护 cipher→uid 检索缓存）',
    email              VARCHAR(100) NULL                     COMMENT '邮箱地址',
    password_hash      VARCHAR(255) NULL                     COMMENT 'BCrypt 密码散列（10轮，微信/手机号登录可为NULL）',
    user_type          TINYINT      NOT NULL DEFAULT 1       COMMENT '用户类型：1-学生 2-教师 3-院系管理员 4-校级管理员',
    dept_id            BIGINT       NULL                     COMMENT '所属院系ID → sys_dept.id',
    open_id            VARCHAR(64)  NULL                     COMMENT '微信 openId（微信登录绑定，唯一）',
    union_id           VARCHAR(64)  NULL                     COMMENT '微信 unionId（跨应用唯一标识）',
    avatar_url         VARCHAR(500) NULL                     COMMENT '头像文件路径（存 MinIO）',
    status             TINYINT      NOT NULL DEFAULT 1       COMMENT '账号状态：0-禁用 1-正常 2-待审核',
    is_deleted         TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-正常 1-已删除',
    last_login_at      DATETIME     NULL                     COMMENT '最后登录时间',
    jwxt_uid           VARCHAR(50)  NULL                     COMMENT '教务系统原始用户ID（ETL同步对照键，建独立索引）',
    archive_photo_path VARCHAR(500) NULL                     COMMENT '档案照 MinIO 加密路径（AES-256加密对象，人脸核验用）',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '创建时间',
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username   (username),
    UNIQUE KEY uk_student_no (student_no),
    UNIQUE KEY uk_open_id    (open_id),
    INDEX idx_dept_id   (dept_id),
    INDEX idx_user_type (user_type),
    INDEX idx_status    (status),
    INDEX idx_jwxt_uid  (jwxt_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='用户主表：学生/教师/管理员（手机号AES-256加密存储）';

CREATE TABLE user_role (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    user_id    BIGINT      NOT NULL                COMMENT '用户ID → sys_user.id',
    role_code  VARCHAR(30) NOT NULL                COMMENT '角色编码：SUPER_ADMIN/DEPT_ADMIN/TEACHER/STUDENT',
    dept_id    BIGINT      NULL                    COMMENT '角色关联院系（院系管理员指定范围）→ sys_dept.id',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_code, dept_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='用户角色关联表（一用户可持多角色）';

CREATE TABLE user_wechat (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    user_id         BIGINT       NOT NULL                COMMENT '用户ID → sys_user.id（唯一）',
    open_id         VARCHAR(64)  NOT NULL                COMMENT '微信小程序 openId',
    union_id        VARCHAR(64)  NULL                    COMMENT '微信开放平台 unionId',
    nick_name       VARCHAR(100) NULL                    COMMENT '微信昵称',
    subscribe_types VARCHAR(200) NULL                    COMMENT '已订阅消息模板ID列表（JSON数组）',
    bound_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id),
    UNIQUE KEY uk_open_id (open_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='用户微信绑定信息（分离主表）';

-- ════════════════════════════════════════════════════════════
-- 课程域 course_*（7张）
-- ════════════════════════════════════════════════════════════

CREATE TABLE course (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '课程ID',
    course_code     VARCHAR(30)  NOT NULL                COMMENT '课程编码（教务系统对照码，唯一）',
    course_name     VARCHAR(100) NOT NULL                COMMENT '课程名称',
    dept_id         BIGINT       NOT NULL                COMMENT '开课院系ID → sys_dept.id',
    credit          DECIMAL(3,1) NOT NULL DEFAULT 0.0   COMMENT '学分',
    course_type     TINYINT      NOT NULL DEFAULT 1      COMMENT '课程类型：1-必修 2-选修 3-实验 4-实践',
    semester        VARCHAR(20)  NOT NULL                COMMENT '学期标识，格式：2025-2026-1',
    description     TEXT         NULL                    COMMENT '课程简介',
    is_deleted      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-正常 1-已删除',
    jwxt_course_id  VARCHAR(50)  NULL                    COMMENT '教务系统课程原始ID（ETL同步对照键）',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_code    (course_code),
    INDEX idx_dept_id            (dept_id),
    INDEX idx_semester           (semester),
    INDEX idx_course_type        (course_type),
    INDEX idx_jwxt_course_id     (jwxt_course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课程基本信息表（一门课可对应多个教学班）';

CREATE TABLE class_room (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '教学班ID',
    course_id      BIGINT       NOT NULL                COMMENT '课程ID → course.id',
    teacher_id     BIGINT       NOT NULL                COMMENT '主讲教师ID → sys_user.id',
    class_name     VARCHAR(100) NOT NULL                COMMENT '教学班名称',
    class_code     VARCHAR(30)  NOT NULL                COMMENT '教学班编码（唯一）',
    semester       VARCHAR(20)  NOT NULL                COMMENT '学期标识，格式：2025-2026-1',
    student_count  INT          NOT NULL DEFAULT 0      COMMENT '选课学生人数（冗余字段，由定时任务维护）',
    dept_id        BIGINT       NOT NULL                COMMENT '开课院系ID → sys_dept.id',
    status         TINYINT      NOT NULL DEFAULT 1      COMMENT '班级状态：0-已归档 1-进行中',
    is_deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-正常 1-已删除',
    jwxt_class_id  VARCHAR(50)  NULL                    COMMENT '教务系统教学班原始ID（ETL同步键）',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '创建时间',
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_code    (class_code),
    INDEX idx_course_id         (course_id),
    INDEX idx_teacher_id        (teacher_id),
    INDEX idx_dept_id           (dept_id),
    INDEX idx_semester          (semester),
    INDEX idx_status            (status),
    INDEX idx_jwxt_class_id     (jwxt_class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='教学班表（一门课程的具体开班）';

CREATE TABLE class_student (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    class_id    BIGINT      NOT NULL                COMMENT '教学班ID → class_room.id',
    student_id  BIGINT      NOT NULL                COMMENT '学生ID → sys_user.id',
    student_no  VARCHAR(20) NOT NULL                COMMENT '学号（冗余，减少关联查询）',
    status      TINYINT     NOT NULL DEFAULT 1      COMMENT '选课状态：0-退课 1-正常选课',
    joined_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_student (class_id, student_id),
    INDEX idx_student_id (student_id),
    INDEX idx_class_id   (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='班级学生关联表（多对多；分组关系见 class_group_student）';

CREATE TABLE class_group (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分组ID',
    class_id    BIGINT      NOT NULL                COMMENT '教学班ID → class_room.id',
    lesson_id   BIGINT      NULL                    COMMENT '关联课堂ID（课上临时分组才有值）→ lesson.id',
    group_name  VARCHAR(50) NOT NULL                COMMENT '分组名称（如"第一组"）',
    group_no    INT         NOT NULL DEFAULT 1      COMMENT '分组序号',
    group_type  VARCHAR(20) NOT NULL DEFAULT 'LESSON' COMMENT '分组范围：CLASS-班级固定分组 LESSON-课堂临时分组',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_class_id  (class_id),
    INDEX idx_lesson_id (lesson_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='分组主表（支持班级固定分组和课堂临时分组两种模式）';

CREATE TABLE class_group_student (
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    group_id   BIGINT   NOT NULL                COMMENT '分组ID → class_group.id',
    student_id BIGINT   NOT NULL                COMMENT '学生ID → sys_user.id',
    class_id   BIGINT   NOT NULL                COMMENT '教学班ID → class_room.id（冗余，加速查询）',
    joined_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入分组时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_student (group_id, student_id),
    INDEX idx_group_id   (group_id),
    INDEX idx_student_id (student_id),
    INDEX idx_class_id   (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='分组成员关联表（从 class_student 抽离，支持高频临时分组变更）';

CREATE TABLE lesson (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '课堂ID',
    class_id         BIGINT       NOT NULL                COMMENT '教学班ID → class_room.id',
    teacher_id       BIGINT       NOT NULL                COMMENT '授课教师ID → sys_user.id',
    material_id      BIGINT       NULL                    COMMENT '课件ID → course_material.id',
    title            VARCHAR(200) NULL                    COMMENT '课堂标题（默认课程名+日期）',
    status           TINYINT      NOT NULL DEFAULT 0      COMMENT '课堂状态：0-未开始 1-进行中 2-已结束',
    start_time       DATETIME     NULL                    COMMENT '实际开课时间',
    end_time         DATETIME     NULL                    COMMENT '实际结课时间',
    live_mode        VARCHAR(20)  NOT NULL DEFAULT 'SLIDE_ONLY' COMMENT '直播模式：SLIDE_ONLY-仅课件 ONLINE_CLASS-线上直播',
    live_push_url    VARCHAR(500) NULL                    COMMENT '直播推流地址（RTMP，线上模式）',
    live_play_url    VARCHAR(500) NULL                    COMMENT '直播拉流地址（HLS）',
    replay_url       VARCHAR(500) NULL                    COMMENT '录播回放文件路径（MinIO）',
    replay_visible   TINYINT      NOT NULL DEFAULT 1      COMMENT '回放是否对学生可见：0-隐藏 1-可见',
    chapter          VARCHAR(200) NULL                    COMMENT '当前章节信息',
    current_slide    INT          NOT NULL DEFAULT 1      COMMENT '当前放映页码（WebSocket同步用）',
    is_scheduled     TINYINT      NOT NULL DEFAULT 0      COMMENT '是否预约课堂：0-即时 1-预约',
    is_deleted       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-正常 1-已删除',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '创建时间',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_class_id   (class_id),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_start_time (start_time),
    INDEX idx_status     (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课堂记录表（每次开课一条记录）';

CREATE TABLE lesson_schedule (
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '预约ID',
    class_id       BIGINT      NOT NULL                COMMENT '教学班ID → class_room.id',
    teacher_id     BIGINT      NOT NULL                COMMENT '教师ID → sys_user.id',
    scheduled_at   DATETIME    NOT NULL                COMMENT '预约上课时间',
    repeat_type    VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT '重复类型：NONE-单次 WEEKLY-每周 BI_WEEKLY-隔周 MONTHLY-每月',
    repeat_end_at  DATETIME    NULL                    COMMENT '重复截止日期',
    week_day       TINYINT     NULL                    COMMENT '星期几（1=周一...7=周日）',
    lesson_id      BIGINT      NULL                    COMMENT '已生成的课堂ID → lesson.id',
    status         TINYINT     NOT NULL DEFAULT 0      COMMENT '状态：0-待开课 1-已开课 2-已取消',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_class_id    (class_id),
    INDEX idx_teacher_id  (teacher_id),
    INDEX idx_scheduled_at (scheduled_at),
    INDEX idx_status      (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课堂预约/排课表（支持周期性课堂）';

CREATE TABLE course_material (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '课件ID',
    teacher_id    BIGINT        NOT NULL                COMMENT '上传教师ID → sys_user.id',
    title         VARCHAR(200)  NOT NULL                COMMENT '课件标题',
    file_type     VARCHAR(20)   NOT NULL                COMMENT '原始文件类型：pptx/pdf/docx/mp4',
    original_path VARCHAR(500)  NOT NULL                COMMENT '原始文件 MinIO 路径',
    slide_dir     VARCHAR(500)  NULL                    COMMENT '转换后图片序列目录路径（MinIO）',
    page_count    INT           NOT NULL DEFAULT 0      COMMENT '课件总页数',
    file_size_kb  INT UNSIGNED  NOT NULL DEFAULT 0      COMMENT '文件大小（KB，INT UNSIGNED 最大约 4.3TB）',
    status        TINYINT       NOT NULL DEFAULT 1      COMMENT '审核状态：0-待审核（视频） 1-可用 2-审核拒绝',
    is_deleted    TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-正常 1-已删除',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '上传时间',
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_status     (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课件库表（含PPT/PDF/视频，视频需审核；file_size_kb 改为 INT UNSIGNED）';

-- ════════════════════════════════════════════════════════════
-- 互动域 attendance_* / interact_*（7张）
-- ════════════════════════════════════════════════════════════

CREATE TABLE attendance_code (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '签到码ID',
    lesson_id   BIGINT      NOT NULL                COMMENT '课堂ID → lesson.id',
    code        VARCHAR(8)  NOT NULL                COMMENT '4~8位口令（随机生成）',
    qr_token    VARCHAR(64) NOT NULL                COMMENT '二维码内嵌 UUID Token',
    expire_at   DATETIME    NOT NULL                COMMENT '过期时间（默认5分钟）',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_lesson_active (lesson_id),
    INDEX idx_qr_token (qr_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课堂签到码表（每课堂只有一个有效签到码，主要依赖 Redis TTL 快速验证）';

CREATE TABLE attendance (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '签到记录ID',
    lesson_id    BIGINT      NOT NULL                COMMENT '课堂ID → lesson.id',
    student_id   BIGINT      NOT NULL                COMMENT '学生ID → sys_user.id',
    class_id     BIGINT      NOT NULL                COMMENT '教学班ID → class_room.id（冗余，加速统计）',
    status       TINYINT     NOT NULL DEFAULT 1      COMMENT '考勤状态：0-缺勤 1-正常签到 2-迟到 3-请假（教师修改）',
    method       VARCHAR(20) NOT NULL DEFAULT 'QR'  COMMENT '签到方式：QR-扫码 CODE-口令 MANUAL-教师手动补签',
    attended_at  DATETIME    NULL                    COMMENT '签到时间（教师手动补签可为NULL）',
    ip_address   VARCHAR(45) NULL                    COMMENT '签到IP地址（IPv4/IPv6）',
    is_modified  TINYINT     NOT NULL DEFAULT 0      COMMENT '教师是否修改过考勤：0-原始 1-已修改',
    modifier_id  BIGINT      NULL                    COMMENT '修改人ID（教师）→ sys_user.id',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间（批量异步落库时间）',
    PRIMARY KEY (id),
    INDEX idx_lesson_student (lesson_id, student_id),
    INDEX idx_lesson_id      (lesson_id),
    INDEX idx_student_id     (student_id),
    INDEX idx_class_id       (class_id),
    INDEX idx_attended_at    (attended_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='签到记录表（异步批量落库；唯一性由 Redis BloomFilter 保证，非 DB UNIQUE 约束）';

CREATE TABLE barrage (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '弹幕ID',
    lesson_id   BIGINT       NOT NULL                COMMENT '课堂ID → lesson.id',
    student_id  BIGINT       NOT NULL                COMMENT '发送学生ID → sys_user.id（后台实名）',
    content     VARCHAR(500) NOT NULL                COMMENT '弹幕内容（前台匿名展示）',
    style       VARCHAR(50)  NOT NULL DEFAULT 'roll' COMMENT '弹幕样式：roll-滚动 top-顶部 bottom-底部',
    is_blocked  TINYINT      NOT NULL DEFAULT 0      COMMENT '是否被教师屏蔽：0-正常 1-屏蔽',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (id),
    INDEX idx_lesson_id_time (lesson_id, created_at),
    INDEX idx_student_id     (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课堂弹幕表（前台匿名展示，后台含学生ID实名）';

CREATE TABLE random_call (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    lesson_id   BIGINT      NOT NULL                COMMENT '课堂ID → lesson.id',
    teacher_id  BIGINT      NOT NULL                COMMENT '操作教师ID → sys_user.id',
    student_ids TEXT        NOT NULL                COMMENT '被点到的学生ID列表（JSON数组）',
    style       VARCHAR(30) NOT NULL DEFAULT 'random' COMMENT '点名样式：random/spotlight/racing',
    called_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点名时间',
    PRIMARY KEY (id),
    INDEX idx_lesson_id (lesson_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='随机点名记录表（每次点名一条，支持多种样式）';

CREATE TABLE slide_feedback (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
    lesson_id     BIGINT       NOT NULL                COMMENT '课堂ID → lesson.id',
    student_id    BIGINT       NOT NULL                COMMENT '学生ID → sys_user.id',
    slide_page    INT          NOT NULL                COMMENT '反馈的课件页码（从1开始）',
    keyword       VARCHAR(100) NOT NULL                COMMENT '标注关键词或疑问内容',
    feedback_type TINYINT      NOT NULL DEFAULT 1      COMMENT '反馈类型：1-疑问 2-关键词标注 3-重点标记',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '反馈时间',
    PRIMARY KEY (id),
    INDEX idx_lesson_slide (lesson_id, slide_page),
    INDEX idx_student_id   (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课件页面反馈表（学生对每页课件的实时标注/疑问）';

CREATE TABLE class_score (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '积分记录ID',
    lesson_id   BIGINT        NOT NULL                COMMENT '课堂ID → lesson.id',
    student_id  BIGINT        NOT NULL                COMMENT '学生ID → sys_user.id',
    class_id    BIGINT        NOT NULL                COMMENT '教学班ID → class_room.id（冗余）',
    score       DECIMAL(5,2)  NOT NULL DEFAULT 0.00   COMMENT '加分值',
    reason      VARCHAR(200)  NULL                    COMMENT '加分原因（教师填写）',
    operator_id BIGINT        NOT NULL                COMMENT '操作教师ID → sys_user.id',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加分时间',
    PRIMARY KEY (id),
    INDEX idx_lesson_student (lesson_id, student_id),
    INDEX idx_class_id       (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课堂积分表（教师对学生课堂表现加分，汇总入 student_grade）';

CREATE TABLE lesson_report (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '报告ID',
    lesson_id       BIGINT        NOT NULL                COMMENT '课堂ID → lesson.id（唯一）',
    attend_count    INT           NOT NULL DEFAULT 0      COMMENT '签到人数',
    absent_count    INT           NOT NULL DEFAULT 0      COMMENT '缺勤人数',
    attend_rate     DECIMAL(5,2)  NOT NULL DEFAULT 0.00   COMMENT '到课率（百分比，如 92.50）',
    interact_count  INT           NOT NULL DEFAULT 0      COMMENT '课堂互动总次数（弹幕+答题+点名）',
    quiz_count      INT           NOT NULL DEFAULT 0      COMMENT '发布题目总数',
    duration_min    INT           NOT NULL DEFAULT 0      COMMENT '课堂实际时长（分钟）',
    slide_count     INT           NOT NULL DEFAULT 0      COMMENT '课件总页数',
    ai_summary      TEXT          NULL                    COMMENT 'AI生成的课堂讲稿摘要（ASR→LLM，异步生成）',
    ai_mindmap_json MEDIUMTEXT    NULL                    COMMENT 'AI生成的思维导图JSON数据（Markmap格式）',
    mindmap_visible TINYINT       NOT NULL DEFAULT 0      COMMENT '思维导图是否对学生开放：0-不开放 1-开放',
    summary_visible TINYINT       NOT NULL DEFAULT 0      COMMENT '课堂总结是否对学生开放：0-不开放 1-开放',
    gen_status      TINYINT       NOT NULL DEFAULT 0      COMMENT 'AI内容生成状态：0-待生成 1-生成中 2-已完成 3-生成失败',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '报告生成时间',
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_lesson_id (lesson_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课堂报告表（课堂结束后AI异步生成）';

-- ════════════════════════════════════════════════════════════
-- 题库域 q_*（3张）
-- ════════════════════════════════════════════════════════════

CREATE TABLE question_bank (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题库ID',
    teacher_id  BIGINT       NOT NULL                COMMENT '创建教师ID → sys_user.id',
    dept_id     BIGINT       NULL                    COMMENT '所属院系（院系共享时有值）→ sys_dept.id',
    bank_name   VARCHAR(100) NOT NULL                COMMENT '题库名称',
    description VARCHAR(500) NULL                    COMMENT '题库描述',
    is_public   TINYINT      NOT NULL DEFAULT 0      COMMENT '是否院系公开：0-私有 1-院系共享',
    is_deleted  TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-正常 1-已删除',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_dept_id    (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='题库表（教师私有或院系共享）';

CREATE TABLE question (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    bank_id      BIGINT        NOT NULL                COMMENT '所属题库ID → question_bank.id',
    type         TINYINT       NOT NULL                COMMENT '题型：1-单选 2-多选 3-判断 4-填空 5-主观 6-投票',
    content      TEXT          NOT NULL                COMMENT '题干内容（富文本HTML，支持图片公式）',
    answer       TEXT          NULL                    COMMENT '标准答案（客观题选项标签如"A,C"；主观题参考答案）',
    analysis     TEXT          NULL                    COMMENT '答案解析（富文本HTML）',
    score        DECIMAL(5,2)  NOT NULL DEFAULT 0.00   COMMENT '默认分值（可在试卷中覆盖）',
    difficulty   TINYINT       NOT NULL DEFAULT 3      COMMENT '难度系数：1-极易 2-易 3-中 4-难 5-极难',
    review_rule  TEXT          NULL                    COMMENT 'AI批改规则提示词（主观题，已过 PromptSecurityFilter）',
    creator_id   BIGINT        NOT NULL                COMMENT '创建教师ID → sys_user.id',
    is_deleted   TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-正常 1-已删除',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '创建时间',
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_bank_id    (bank_id),
    INDEX idx_type       (type),
    INDEX idx_difficulty (difficulty),
    FULLTEXT INDEX ft_content (content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='题目主表（支持6种题型，FULLTEXT ngram全文检索；同步至 ES edu_search）';

CREATE TABLE question_option (
    id           BIGINT  NOT NULL AUTO_INCREMENT COMMENT '选项ID',
    question_id  BIGINT  NOT NULL                COMMENT '题目ID → question.id',
    option_label CHAR(1) NOT NULL                COMMENT '选项标签：A/B/C/D/E...',
    content      TEXT    NOT NULL                COMMENT '选项内容（富文本HTML，支持图片）',
    is_correct   TINYINT NOT NULL DEFAULT 0      COMMENT '是否为正确选项：0-错误 1-正确',
    sort_order   TINYINT NOT NULL DEFAULT 1      COMMENT '显示排序（从1开始）',
    PRIMARY KEY (id),
    INDEX idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='题目选项表（单选/多选/投票题的选项列表）';

-- ════════════════════════════════════════════════════════════
-- 测试域 exam_*（6张）
-- ════════════════════════════════════════════════════════════

CREATE TABLE exam_paper (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '试卷ID',
    creator_id  BIGINT        NOT NULL                COMMENT '创建教师ID → sys_user.id',
    title       VARCHAR(200)  NOT NULL                COMMENT '试卷标题',
    total_score DECIMAL(6,2)  NOT NULL DEFAULT 100.00 COMMENT '试卷总分',
    is_random   TINYINT       NOT NULL DEFAULT 0      COMMENT '是否随机组卷：0-固定 1-随机抽题',
    paper_type  VARCHAR(10)   NOT NULL DEFAULT 'A'    COMMENT '试卷卷型：A/B/C（ABC卷各自独立统计）',
    description TEXT          NULL                    COMMENT '试卷说明/考试须知',
    is_deleted  TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-正常 1-已删除',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '创建时间',
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_creator_id (creator_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='试卷主表（一张试卷可多次发布给不同班级）';

CREATE TABLE exam_paper_question (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    paper_id    BIGINT        NOT NULL                COMMENT '试卷ID → exam_paper.id',
    question_id BIGINT        NOT NULL                COMMENT '题目ID → question.id',
    score       DECIMAL(5,2)  NOT NULL                COMMENT '本题在此试卷中的分值（覆盖题目默认分值）',
    sort_order  INT           NOT NULL DEFAULT 1      COMMENT '题目在试卷中的显示顺序',
    paper_group CHAR(1)       NOT NULL DEFAULT 'A'    COMMENT '所属卷组（A/B/C卷，随机组卷时有效）',
    section     VARCHAR(50)   NULL                    COMMENT '大题/章节名称（如"一、单选题"）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_paper_question_group (paper_id, question_id, paper_group),
    INDEX idx_paper_id    (paper_id),
    INDEX idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='试卷题目关联表（含分值覆盖和ABC卷分组）';

CREATE TABLE exam_publish (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '发布ID',
    paper_id         BIGINT       NOT NULL                COMMENT '试卷ID → exam_paper.id',
    class_id         BIGINT       NOT NULL                COMMENT '发布目标班级ID → class_room.id',
    teacher_id       BIGINT       NOT NULL                COMMENT '发布教师ID → sys_user.id',
    start_time       DATETIME     NOT NULL                COMMENT '考试开始时间',
    end_time         DATETIME     NOT NULL                COMMENT '考试截止时间',
    duration_min     INT          NOT NULL DEFAULT 60     COMMENT '考试时长（分钟）',
    password_hash    VARCHAR(255) NULL                    COMMENT '防泄题密码 BCrypt 散列（NULL=不设密码）',
    enable_monitor   TINYINT      NOT NULL DEFAULT 0      COMMENT '是否开启在线监考：0-否 1-是',
    face_verify_type TINYINT      NOT NULL DEFAULT 0      COMMENT '人脸核验方式：0-不核验 1-证件照 2-现场拍照',
    answer_show_at   DATETIME     NULL                    COMMENT '答案/成绩公布时间（NULL=发布后立即可查）',
    allow_copy       TINYINT      NOT NULL DEFAULT 0      COMMENT '是否允许复制粘贴：0-禁止 1-允许',
    shuffle_question TINYINT      NOT NULL DEFAULT 0      COMMENT '是否乱序题目：0-固定 1-随机',
    shuffle_option   TINYINT      NOT NULL DEFAULT 0      COMMENT '是否乱序选项：0-固定 1-随机',
    status           TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0-预约未开始 1-进行中 2-已结束 3-已取消',
    is_deleted       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-正常 1-已删除',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '发布时间',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_paper_id   (paper_id),
    INDEX idx_class_id   (class_id),
    INDEX idx_start_time (start_time),
    INDEX idx_status     (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='试卷发布配置表（一张试卷可多次发布，每次配置独立）';

CREATE TABLE student_answer (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '作答记录ID',
    publish_id     BIGINT        NOT NULL                COMMENT '发布ID → exam_publish.id',
    question_id    BIGINT        NOT NULL                COMMENT '题目ID → question.id',
    student_id     BIGINT        NOT NULL                COMMENT '学生ID → sys_user.id',
    answer_content TEXT          NULL                    COMMENT '作答内容（富文本/JSON，主观题可含图片路径）',
    score          DECIMAL(5,2)  NULL                    COMMENT '得分（NULL=未批改）',
    is_correct     TINYINT       NULL                    COMMENT '是否正确（客观题）：NULL-未批改 0-错误 1-正确',
    comment        TEXT          NULL                    COMMENT '教师/AI批注内容',
    review_status  TINYINT       NOT NULL DEFAULT 0      COMMENT '批改状态：0-未批改 1-AI已批改 2-教师已批改',
    submitted_at   DATETIME      NULL                    COMMENT '作答提交时间',
    is_deleted     TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-正常 1-已删除',
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '记录创建时间',
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_publish_question_student (publish_id, question_id, student_id),
    INDEX idx_student_id    (student_id),
    INDEX idx_publish_id    (publish_id),
    INDEX idx_review_status (review_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='学生作答记录表（每题一条，由交卷流水表异步落库）';

CREATE TABLE exam_monitor (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '监考记录ID',
    publish_id        BIGINT       NOT NULL                COMMENT '发布ID → exam_publish.id',
    student_id        BIGINT       NOT NULL                COMMENT '学生ID → sys_user.id',
    session_status    VARCHAR(20)  NOT NULL DEFAULT 'ANSWERING' COMMENT '考试会话状态：VERIFYING-身份核验中 ANSWERING-作答中 SUBMITTED-已交卷 OFFLINE-断线 ABNORMAL-标记异常',
    last_heartbeat_at DATETIME     NULL                    COMMENT '最近一次心跳时间（前端每30s上报，判断是否在线）',
    face_verify_passed TINYINT     NULL                    COMMENT '人脸核验结果：NULL-未核验 0-未通过 1-通过',
    face_verify_score  DECIMAL(5,2) NULL                  COMMENT '人脸比对置信度分值（0~100）',
    tab_switch_count  INT          NOT NULL DEFAULT 0      COMMENT '切换Tab/失焦次数',
    screenshot_count  INT          NOT NULL DEFAULT 0      COMMENT '检测到截屏次数',
    copy_count        INT          NOT NULL DEFAULT 0      COMMENT '复制粘贴次数',
    abnormal_flag     TINYINT      NOT NULL DEFAULT 0      COMMENT '是否标记为异常：0-正常 1-已标记（供教师重点关注）',
    snapshot_url      VARCHAR(500) NULL                    COMMENT '最新截图存证路径（MinIO）',
    submit_time       DATETIME     NULL                    COMMENT '实际交卷时间',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '进入考试时间',
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_publish_student (publish_id, student_id),
    INDEX idx_publish_id      (publish_id),
    INDEX idx_student_id      (student_id),
    INDEX idx_session_status  (session_status),
    INDEX idx_abnormal_flag   (abnormal_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='监考状态表（每学生一行，实时反映考试会话状态与异常行为累计）';

CREATE TABLE exam_submit_queue (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '流水ID',
    publish_id       BIGINT       NOT NULL                COMMENT '发布ID → exam_publish.id',
    student_id       BIGINT       NOT NULL                COMMENT '学生ID → sys_user.id',
    answers_json     MEDIUMTEXT   NOT NULL                COMMENT '答题快照 JSON（含全部题目作答）',
    submit_type      VARCHAR(20)  NOT NULL DEFAULT 'MANUAL' COMMENT '交卷类型：MANUAL-主动交卷 AUTO-倒计时自动交卷 FORCE-教师强制收卷',
    client_submit_at DATETIME     NOT NULL                COMMENT '客户端提交时间（学号打散后的实际触发时间）',
    process_status   TINYINT      NOT NULL DEFAULT 0      COMMENT '处理状态：0-待处理 1-处理中 2-已展开到明细表 3-处理失败',
    retry_count      TINYINT      NOT NULL DEFAULT 0      COMMENT '重试次数',
    error_msg        VARCHAR(500) NULL                    COMMENT '处理失败原因',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入队时间（Kafka Consumer 落库时间）',
    processed_at     DATETIME     NULL                    COMMENT '处理完成时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_publish_student (publish_id, student_id),
    INDEX idx_process_status (process_status),
    INDEX idx_publish_id     (publish_id),
    INDEX idx_created_at     (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='交卷流水暂存表（承载高并发交卷容灾；Kafka消费落库后再异步展开到 student_answer）';

-- ════════════════════════════════════════════════════════════
-- 成绩域 grade_*（4张）
-- ════════════════════════════════════════════════════════════

CREATE TABLE grade_rule (
    id                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    class_id            BIGINT        NOT NULL                COMMENT '教学班ID → class_room.id（唯一）',
    attend_weight       DECIMAL(5,2)  NOT NULL DEFAULT 20.00  COMMENT '课堂考勤权重（%，所有权重合计=100）',
    quiz_weight         DECIMAL(5,2)  NOT NULL DEFAULT 20.00  COMMENT '课堂小测权重（%）',
    interaction_weight  DECIMAL(5,2)  NOT NULL DEFAULT 10.00  COMMENT '课堂互动积分权重（%）',
    exam_weight         DECIMAL(5,2)  NOT NULL DEFAULT 40.00  COMMENT '在线考试权重（%）',
    offline_weight      DECIMAL(5,2)  NOT NULL DEFAULT 10.00  COMMENT '线下成绩权重（%）',
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '创建时间',
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_id (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='成绩评分规则表（每班一份，各维度权重合计100%，由应用层校验）';

CREATE TABLE student_grade (
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '成绩ID',
    class_id          BIGINT        NOT NULL                COMMENT '教学班ID → class_room.id',
    student_id        BIGINT        NOT NULL                COMMENT '学生ID → sys_user.id',
    total_score       DECIMAL(6,2)  NULL                    COMMENT '综合总分（按权重计算，NULL=待计算）',
    attend_score      DECIMAL(6,2)  NOT NULL DEFAULT 0.00   COMMENT '考勤维度得分',
    quiz_score        DECIMAL(6,2)  NOT NULL DEFAULT 0.00   COMMENT '课堂小测维度得分',
    interaction_score DECIMAL(6,2)  NOT NULL DEFAULT 0.00   COMMENT '互动积分维度得分',
    exam_score        DECIMAL(6,2)  NOT NULL DEFAULT 0.00   COMMENT '在线考试维度得分',
    offline_score     DECIMAL(6,2)  NULL                    COMMENT '线下成绩（教师导入，NULL=未导入）',
    calc_status       TINYINT       NOT NULL DEFAULT 0      COMMENT '计算状态：0-未计算 1-已计算',
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后计算时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_student (class_id, student_id),
    INDEX idx_class_id    (class_id),
    INDEX idx_student_id  (student_id),
    INDEX idx_total_score (total_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='学生综合成绩汇总表（由 GradeCalculator 定时/触发计算）';

CREATE TABLE grade_detail (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    class_id      BIGINT        NOT NULL                COMMENT '教学班ID → class_room.id',
    student_id    BIGINT        NOT NULL                COMMENT '学生ID → sys_user.id',
    score_type    VARCHAR(30)   NOT NULL                COMMENT '成绩来源类型：ATTEND/QUIZ/INTERACTION/EXAM/OFFLINE',
    source_id     BIGINT        NULL                    COMMENT '来源记录ID（lesson_id/publish_id等）',
    score         DECIMAL(6,2)  NOT NULL                COMMENT '本次得分',
    remark        VARCHAR(200)  NULL                    COMMENT '备注',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (id),
    INDEX idx_class_student (class_id, student_id),
    INDEX idx_score_type    (score_type),
    INDEX idx_student_id    (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='成绩明细表（各维度得分明细，用于成绩溯源）';

CREATE TABLE grade_offline (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    class_id    BIGINT        NOT NULL                COMMENT '教学班ID → class_room.id',
    student_id  BIGINT        NOT NULL                COMMENT '学生ID → sys_user.id',
    score       DECIMAL(6,2)  NOT NULL                COMMENT '线下成绩分值',
    remark      VARCHAR(200)  NULL                    COMMENT '备注（如：期末卷面分）',
    operator_id BIGINT        NOT NULL                COMMENT '导入教师ID → sys_user.id',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_student (class_id, student_id),
    INDEX idx_class_id (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='线下成绩导入表（教师手动录入线下考试成绩）';

-- ════════════════════════════════════════════════════════════
-- 文件域 file_*（3张）
-- ════════════════════════════════════════════════════════════

CREATE TABLE file_object (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    uploader_id     BIGINT        NOT NULL                COMMENT '上传用户ID → sys_user.id',
    file_name       VARCHAR(255)  NOT NULL                COMMENT '原始文件名',
    file_type       VARCHAR(50)   NOT NULL                COMMENT 'MIME类型（如 image/png, video/mp4）',
    file_size_kb    INT UNSIGNED  NOT NULL                COMMENT '文件大小（KB，INT UNSIGNED 最大约 4TB）',
    bucket_name     VARCHAR(100)  NOT NULL DEFAULT 'edu-files' COMMENT 'MinIO Bucket名称',
    object_path     VARCHAR(500)  NOT NULL                COMMENT 'MinIO对象路径（不含Bucket）',
    cdn_url         VARCHAR(500)  NULL                    COMMENT 'CDN加速URL（视频类文件）',
    biz_type        VARCHAR(50)   NOT NULL                COMMENT '业务类型：SLIDE/VIDEO/AUDIO/EXAM_ATTACH/ARCHIVE_PHOTO',
    storage_class   VARCHAR(20)   NOT NULL DEFAULT 'HOT'  COMMENT '存储级别：HOT-热存储 COLD-冷存储',
    audit_status    TINYINT       NOT NULL DEFAULT 1      COMMENT '审核状态：0-待审核 1-通过 2-拒绝',
    lifecycle_stage VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT '生命周期阶段：ACTIVE-活跃 ARCHIVED-归档 DELETED-已删除',
    expire_at       DATETIME      NULL                    COMMENT '计划归档/删除时间',
    is_deleted      TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-正常 1-已删除',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '上传时间',
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_uploader_id     (uploader_id),
    INDEX idx_biz_type        (biz_type),
    INDEX idx_lifecycle_stage (lifecycle_stage),
    INDEX idx_expire_at       (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='文件对象元信息表（本体存MinIO，此表存元数据及生命周期状态）';

CREATE TABLE live_record (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '直播记录ID',
    lesson_id    BIGINT       NOT NULL                COMMENT '课堂ID → lesson.id',
    stream_key   VARCHAR(100) NOT NULL                COMMENT 'RTMP 推流密钥',
    push_url     VARCHAR(500) NOT NULL                COMMENT 'RTMP 推流地址',
    play_url     VARCHAR(500) NULL                    COMMENT 'HLS 拉流播放地址',
    replay_path  VARCHAR(500) NULL                    COMMENT '录播文件 MinIO 路径',
    duration_sec INT          NOT NULL DEFAULT 0      COMMENT '直播时长（秒）',
    status       TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0-待推流 1-推流中 2-已结束 3-已生成回放',
    started_at   DATETIME     NULL                    COMMENT '实际推流开始时间',
    ended_at     DATETIME     NULL                    COMMENT '推流结束时间',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_lesson_id (lesson_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='直播推流记录表（仅 ONLINE_CLASS 模式生成，SLIDE_ONLY 模式不创建）';

CREATE TABLE exam_attachment (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '附件ID',
    publish_id   BIGINT       NOT NULL                COMMENT '发布ID → exam_publish.id',
    student_id   BIGINT       NOT NULL                COMMENT '学生ID → sys_user.id',
    question_id  BIGINT       NOT NULL                COMMENT '题目ID → question.id',
    file_id      BIGINT       NOT NULL                COMMENT '文件ID → file_object.id',
    upload_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (id),
    INDEX idx_publish_student (publish_id, student_id),
    INDEX idx_file_id         (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='考试附件关联表（主观题图片答案关联）';

-- ════════════════════════════════════════════════════════════
-- 通知域 notify_*（3张）
-- ════════════════════════════════════════════════════════════

CREATE TABLE notice (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    sender_id    BIGINT       NOT NULL                COMMENT '发送人ID → sys_user.id',
    sender_name  VARCHAR(50)  NOT NULL                COMMENT '发送人显示名称（支持自定义）',
    title        VARCHAR(200) NOT NULL                COMMENT '通知标题',
    content      TEXT         NOT NULL                COMMENT '通知内容（富文本HTML）',
    scope        VARCHAR(20)  NOT NULL                COMMENT '发送范围：SCHOOL-全校 DEPT-指定院系 CLASS-指定班级',
    dept_id      BIGINT       NULL                    COMMENT '指定院系ID（scope=DEPT时有效）→ sys_dept.id',
    target_roles VARCHAR(100) NULL                    COMMENT '目标角色过滤：ALL/TEACHER/STUDENT',
    need_review  TINYINT      NOT NULL DEFAULT 0      COMMENT '是否需要审核：0-直接发布 1-需审核',
    status       TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0-草稿 1-审核中 2-已发布 3-已撤回',
    send_count   INT          NOT NULL DEFAULT 0      COMMENT '发送总人数',
    read_count   INT          NOT NULL DEFAULT 0      COMMENT '已读人数',
    published_at DATETIME     NULL                    COMMENT '发布时间',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_sender_id    (sender_id),
    INDEX idx_status       (status),
    INDEX idx_published_at (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='通知公告表（支持全校/院系/班级三级范围推送）';

CREATE TABLE notice_read (
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    notice_id  BIGINT   NOT NULL                COMMENT '通知ID → notice.id',
    user_id    BIGINT   NOT NULL                COMMENT '用户ID → sys_user.id',
    read_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_notice_user (notice_id, user_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='通知已读记录表';

CREATE TABLE msg_push_token (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    user_id     BIGINT       NOT NULL                COMMENT '用户ID → sys_user.id',
    platform    VARCHAR(20)  NOT NULL                COMMENT '推送平台：WECHAT/WEB_PUSH',
    token       VARCHAR(500) NOT NULL                COMMENT '设备推送 Token（微信 openId 或 WebPush subscription JSON）',
    is_active   TINYINT      NOT NULL DEFAULT 1      COMMENT '是否有效：0-失效 1-有效',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_platform (user_id, platform),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='消息推送 Token 表（微信订阅消息/WebPush 设备标识）';

-- ════════════════════════════════════════════════════════════
-- 教务对接域 jwxt_*（3张）
-- ════════════════════════════════════════════════════════════

CREATE TABLE jwxt_sync_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '同步日志ID',
    sync_type       VARCHAR(20)  NOT NULL                COMMENT '同步类型：FULL-全量 INCREMENTAL-增量',
    sync_date       DATE         NOT NULL                COMMENT '同步数据基准日期',
    student_cnt     INT          NOT NULL DEFAULT 0      COMMENT '同步学生记录数',
    dept_cnt        INT          NOT NULL DEFAULT 0      COMMENT '同步院系/班级记录数',
    course_cnt      INT          NOT NULL DEFAULT 0      COMMENT '同步课程/教学班记录数',
    success_cnt     INT          NOT NULL DEFAULT 0      COMMENT '成功处理条数',
    failed_cnt      INT          NOT NULL DEFAULT 0      COMMENT '失败条数',
    status          TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0-进行中 1-成功 2-部分失败 3-完全失败',
    error_msg       TEXT         NULL                    COMMENT '失败原因摘要',
    cost_ms         BIGINT       NULL                    COMMENT '总耗时（毫秒）',
    triggered_by    VARCHAR(50)  NOT NULL DEFAULT 'SCHEDULE' COMMENT '触发方式：SCHEDULE-定时 MANUAL-手动',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '同步开始时间',
    finished_at     DATETIME     NULL                    COMMENT '同步完成时间',
    PRIMARY KEY (id),
    INDEX idx_sync_date  (sync_date),
    INDEX idx_status     (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='教务系统同步日志表（全量/增量ETL执行记录）';

CREATE TABLE jwxt_raw_data (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    sync_log_id   BIGINT       NOT NULL                COMMENT '同步日志ID → jwxt_sync_log.id',
    data_type     VARCHAR(20)  NOT NULL                COMMENT '数据类型：STUDENT/DEPT/COURSE/CLASS',
    jwxt_id       VARCHAR(50)  NOT NULL                COMMENT '教务系统原始ID（便于快速对照）',
    raw_json      JSON         NOT NULL                COMMENT '教务系统原始数据（JSON格式）',
    status        TINYINT      NOT NULL DEFAULT 0      COMMENT '处理状态：0-待处理 1-成功 2-失败',
    error_msg     TEXT         NULL                    COMMENT '处理失败原因',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (id),
    INDEX idx_sync_log_id      (sync_log_id),
    INDEX idx_data_type_status (data_type, status),
    INDEX idx_jwxt_id          (jwxt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='教务原始数据暂存表（ETL过渡，成功处理90天后清理）';

CREATE TABLE jwxt_id_mapping (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '映射记录ID',
    data_type   VARCHAR(20) NOT NULL                COMMENT '数据类型：USER/DEPT/COURSE/CLASS',
    jwxt_id     VARCHAR(50) NOT NULL                COMMENT '教务系统原始ID',
    local_id    BIGINT      NOT NULL                COMMENT '本系统对应主键（sys_user.id / sys_dept.id 等）',
    sync_log_id BIGINT      NULL                    COMMENT '最后同步的日志ID → jwxt_sync_log.id',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '首次映射时间',
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_jwxt_id  (data_type, jwxt_id),
    UNIQUE KEY uk_type_local_id (data_type, local_id),
    INDEX idx_sync_log_id       (sync_log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='教务系统ID对照映射表（ETL增量同步核心，双向唯一索引，O(1)查找）';
