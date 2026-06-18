-- 开发环境种子数据
-- 对应 CLAUDE.md §3.5 测试账号：admin/edu2026@admin, teacher01/edu2026@test, student01/edu2026@test
-- 密码 BCrypt(rounds=10) 生成

USE edu_db;

-- ─── 院系基础数据 ────────────────────────────────────────────────────────────────
INSERT IGNORE INTO sys_dept (id, dept_code, dept_name, parent_id, dept_type, level, sort_order)
VALUES
  (1,  'ROOT',    '山东管理学院',       NULL, 1, 1, 0),
  (2,  'CS',      '计算机学院',         1,    2, 2, 1),
  (3,  'EE',      '电子工程学院',       1,    2, 2, 2),
  (4,  'BUS',     '商学院',             1,    2, 2, 3),
  (10, 'ADMIN',   '教务处',             1,    3, 2, 10);

-- ─── 测试账号 ─────────────────────────────────────────────────────────────────
-- user_type: 1=学生 2=教师 3=管理员
INSERT IGNORE INTO sys_user (id, username, real_name, password_hash, user_type, dept_id, status)
VALUES
  (1, 'admin',     '系统管理员', '$2a$10$mcUIX83km4klazUD/PV.3.LtStcCJ7V6eYzQ2YLLiPYMIFOKIlaA2', 3, 10, 1),
  (2, 'teacher01', '测试教师',   '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 2,  2, 1),
  (3, 'student01', '测试学生',   '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 1,  2, 1);

-- student_no 仅学生填写
UPDATE sys_user SET student_no = '2022010001' WHERE id = 3;

-- ─── 角色分配 ─────────────────────────────────────────────────────────────────
INSERT IGNORE INTO user_role (user_id, role_code, dept_id)
VALUES
  (1, 'ROLE_ADMIN',   10),
  (2, 'ROLE_TEACHER',  2),
  (3, 'ROLE_STUDENT',  2);
