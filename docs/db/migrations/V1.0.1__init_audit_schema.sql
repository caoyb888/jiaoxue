-- ============================================================
-- 山东管理学院智慧教学系统 — edu_audit_db 操作日志表 DDL
-- Flyway 版本：V1.0.1
-- 等保三级：操作日志独立库，留存 ≥ 180 天，禁止 DELETE
-- ============================================================

USE edu_audit_db;

CREATE TABLE log_operation (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    user_id      BIGINT       NOT NULL                COMMENT '操作人ID（sys_user.id，跨库冗余存储）',
    username     VARCHAR(50)  NOT NULL                COMMENT '操作人用户名（冗余，防止 JOIN）',
    dept_id      BIGINT       NULL                    COMMENT '操作人所属院系ID（冗余）',
    module       VARCHAR(50)  NOT NULL                COMMENT '功能模块（@OperationLog.module）',
    operation    VARCHAR(100) NOT NULL                COMMENT '操作描述（@OperationLog.operation）',
    method       VARCHAR(200) NOT NULL                COMMENT 'Controller 方法全路径',
    http_method  VARCHAR(10)  NOT NULL                COMMENT 'HTTP 请求方法：GET/POST/PUT/DELETE',
    request_url  VARCHAR(500) NOT NULL                COMMENT '请求 URL（不含域名）',
    request_ip   VARCHAR(45)  NOT NULL                COMMENT '客户端 IP（IPv4/IPv6）',
    request_body TEXT         NULL                    COMMENT '请求参数 JSON（脱敏处理：手机号/密码字段替换为 ****）',
    response_code INT         NOT NULL                COMMENT '业务响应码（Result.code）',
    error_msg    TEXT         NULL                    COMMENT '异常信息（失败时记录）',
    cost_ms      INT          NOT NULL DEFAULT 0      COMMENT '接口耗时（毫秒）',
    log_date     DATE         NOT NULL                COMMENT '日志日期（分区键，由应用层 INSERT 时填入）',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '日志记录时间',
    PRIMARY KEY (id, log_date),
    INDEX idx_user_id    (user_id),
    INDEX idx_module     (module),
    INDEX idx_log_date   (log_date),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='操作审计日志表（等保三级，禁止 DELETE，180天后归档冷存储）'
  PARTITION BY RANGE (TO_DAYS(log_date)) (
    PARTITION p_2026_01 VALUES LESS THAN (TO_DAYS('2026-02-01')),
    PARTITION p_2026_02 VALUES LESS THAN (TO_DAYS('2026-03-01')),
    PARTITION p_2026_03 VALUES LESS THAN (TO_DAYS('2026-04-01')),
    PARTITION p_2026_04 VALUES LESS THAN (TO_DAYS('2026-05-01')),
    PARTITION p_2026_05 VALUES LESS THAN (TO_DAYS('2026-06-01')),
    PARTITION p_2026_06 VALUES LESS THAN (TO_DAYS('2026-07-01')),
    PARTITION p_2026_07 VALUES LESS THAN (TO_DAYS('2026-08-01')),
    PARTITION p_2026_08 VALUES LESS THAN (TO_DAYS('2026-09-01')),
    PARTITION p_2026_09 VALUES LESS THAN (TO_DAYS('2026-10-01')),
    PARTITION p_2026_10 VALUES LESS THAN (TO_DAYS('2026-11-01')),
    PARTITION p_2026_11 VALUES LESS THAN (TO_DAYS('2026-12-01')),
    PARTITION p_2026_12 VALUES LESS THAN (TO_DAYS('2027-01-01')),
    PARTITION p_2027_01 VALUES LESS THAN (TO_DAYS('2027-02-01')),
    PARTITION p_future   VALUES LESS THAN MAXVALUE
  );
