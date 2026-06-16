-- 创建所有业务数据库
CREATE DATABASE IF NOT EXISTS edu_db          DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS edu_audit_db    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS nacos_config    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS xxl_job         DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- edu_db 业务账号（最小权限，等保三级要求）
CREATE USER IF NOT EXISTS 'edu_app'@'%' IDENTIFIED BY 'edu_app_2026';
GRANT SELECT, INSERT, UPDATE ON edu_db.* TO 'edu_app'@'%';

-- edu_audit_db 审计账号（只允许 INSERT + SELECT，禁止 DELETE/UPDATE，等保三级强制）
CREATE USER IF NOT EXISTS 'edu_audit'@'%' IDENTIFIED BY 'edu_audit_2026';
GRANT SELECT, INSERT ON edu_audit_db.* TO 'edu_audit'@'%';

-- 三员分立：审计管理员账号（只读审计库）
CREATE USER IF NOT EXISTS 'audit_admin'@'%' IDENTIFIED BY 'audit_admin_2026';
GRANT SELECT ON edu_audit_db.* TO 'audit_admin'@'%';

FLUSH PRIVILEGES;
