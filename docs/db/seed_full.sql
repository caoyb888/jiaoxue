-- ============================================================
-- 智慧教学系统 · 完整开发测试数据
-- 执行方式：docker exec -i edu-mysql mysql -u root -pedu_dev_2026 --default-character-set=utf8mb4 edu_db < seed_full.sql
-- 登录方式：调用 POST /api/v1/auth/sms/send?phone=13800000001 后
--           用 POST /api/v1/auth/login/phone 登录（验证码在 Redis 中可查）
-- ============================================================

-- 强制连接字符集为 utf8mb4，避免客户端默认 latin1 导致中文按 latin1 读入而双重编码乱码
SET NAMES utf8mb4;

USE edu_db;

-- ─────────────────────────────────────────────────────────────
-- 1. 院系补全（已有 ROOT/CS/EE/BUS/ADMIN，补充子专业）
-- ─────────────────────────────────────────────────────────────
INSERT IGNORE INTO sys_dept (id, dept_code, dept_name, parent_id, dept_type, level, sort_order)
VALUES
  (5,  'CS_SE',  '软件工程系',   2, 3, 3, 1),
  (6,  'CS_NET', '网络工程系',   2, 3, 3, 2),
  (7,  'EE_IOT', '物联网工程系', 3, 3, 3, 1);

-- ─────────────────────────────────────────────────────────────
-- 2. 用户（phone_cipher 存明文手机号，供开发登录使用）
-- ─────────────────────────────────────────────────────────────
-- 给已有用户补充手机号
UPDATE sys_user SET phone_cipher = '13800000001' WHERE id = 1;
UPDATE sys_user SET phone_cipher = '13800000002' WHERE id = 2;
UPDATE sys_user SET phone_cipher = '13800000011' WHERE id = 3;

-- 追加教师账号（teacher02-04）
INSERT IGNORE INTO sys_user (id, username, real_name, phone_cipher, password_hash, user_type, dept_id, status)
VALUES
  (4,  'teacher02', '李明华', '13800000003', '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 2, 2, 1),
  (5,  'teacher03', '王芳',   '13800000004', '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 2, 3, 1),
  (6,  'teacher04', '赵志强', '13800000005', '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 2, 5, 1);

-- 追加学生账号（student02-15）
INSERT IGNORE INTO sys_user (id, username, real_name, phone_cipher, password_hash, user_type, dept_id, student_no, status)
VALUES
  (12, 'student02', '王小明', '13800000012', '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 1, 5, '2022010002', 1),
  (13, 'student03', '刘思雨', '13800000013', '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 1, 5, '2022010003', 1),
  (14, 'student04', '陈佳佳', '13800000014', '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 1, 5, '2022010004', 1),
  (15, 'student05', '张三丰', '13800000015', '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 1, 5, '2022010005', 1),
  (16, 'student06', '李雪梅', '13800000016', '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 1, 5, '2022010006', 1),
  (17, 'student07', '周浩然', '13800000017', '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 1, 6, '2022020001', 1),
  (18, 'student08', '吴静文', '13800000018', '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 1, 6, '2022020002', 1),
  (19, 'student09', '孙鹏飞', '13800000019', '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 1, 6, '2022020003', 1),
  (20, 'student10', '郑晓华', '13800000020', '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 1, 6, '2022020004', 1),
  (21, 'student11', '冯敏捷', '13800000021', '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 1, 7, '2022030001', 1),
  (22, 'student12', '蒋晨阳', '13800000022', '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 1, 7, '2022030002', 1),
  (23, 'student13', '韩美丽', '13800000023', '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 1, 7, '2022030003', 1),
  (24, 'student14', '唐志远', '13800000024', '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 1, 5, '2022010007', 1),
  (25, 'student15', '秦雨欣', '13800000025', '$2a$10$7Exf/Nl2qV6C5IM3IdH5EuTTqXY1UUcJw2UpZlmNJRsQDL/n5PMuO', 1, 5, '2022010008', 1);

-- 角色分配
INSERT IGNORE INTO user_role (user_id, role_code, dept_id)
VALUES
  (4,  'ROLE_TEACHER', 2),
  (5,  'ROLE_TEACHER', 3),
  (6,  'ROLE_TEACHER', 5),
  (12, 'ROLE_STUDENT', 5),
  (13, 'ROLE_STUDENT', 5),
  (14, 'ROLE_STUDENT', 5),
  (15, 'ROLE_STUDENT', 5),
  (16, 'ROLE_STUDENT', 5),
  (17, 'ROLE_STUDENT', 6),
  (18, 'ROLE_STUDENT', 6),
  (19, 'ROLE_STUDENT', 6),
  (20, 'ROLE_STUDENT', 6),
  (21, 'ROLE_STUDENT', 7),
  (22, 'ROLE_STUDENT', 7),
  (23, 'ROLE_STUDENT', 7),
  (24, 'ROLE_STUDENT', 5),
  (25, 'ROLE_STUDENT', 5);

-- ─────────────────────────────────────────────────────────────
-- 3. 课程
-- course_type: 1=必修 2=选修
-- ─────────────────────────────────────────────────────────────
INSERT IGNORE INTO course (id, course_code, course_name, dept_id, credit, course_type, semester, description)
VALUES
  (1, 'CS101', 'Python程序设计基础',   2, 3.0, 1, '2026-1', 'Python语言基础与应用开发'),
  (2, 'CS201', 'Java面向对象程序设计', 2, 4.0, 1, '2026-1', 'Java核心技术与企业应用'),
  (3, 'CS301', '数据结构与算法',       5, 4.0, 1, '2026-1', '常用数据结构及经典算法分析'),
  (4, 'CS401', 'Web前端开发',          5, 3.0, 2, '2026-1', 'HTML/CSS/JavaScript/React全栈开发'),
  (5, 'EE101', '物联网导论',           7, 2.0, 1, '2026-1', '物联网技术体系及应用场景');

-- ─────────────────────────────────────────────────────────────
-- 4. 班级（class_room）
-- status: 1=活跃 0=归档
-- ─────────────────────────────────────────────────────────────
INSERT IGNORE INTO class_room (id, course_id, teacher_id, class_name, class_code, semester, student_count, dept_id, status)
VALUES
  (1,  1, 2,  'Python基础 - 软件工程2022A班', 'CS101-2022A', '2026-1', 8, 5, 1),
  (2,  1, 2,  'Python基础 - 软件工程2022B班', 'CS101-2022B', '2026-1', 6, 5, 1),
  (3,  2, 4,  'Java面向对象 - 计算机2022A班', 'CS201-2022A', '2026-1', 5, 2, 1),
  (4,  3, 6,  '数据结构 - 软件工程2022A班',   'CS301-2022A', '2026-1', 8, 5, 1),
  (5,  4, 2,  'Web前端 - 软件工程2022A班',    'CS401-2022A', '2026-1', 8, 5, 1),
  (6,  5, 5,  '物联网导论 - 物联网2022A班',   'EE101-2022A', '2026-1', 4, 7, 1);

-- ─────────────────────────────────────────────────────────────
-- 5. 班级学生关系
-- ─────────────────────────────────────────────────────────────
-- class_room 1 (CS101-2022A): students 3,12,13,14,15,16,24,25
INSERT IGNORE INTO class_student (class_id, student_id, student_no)
VALUES
  (1, 3,  '2022010001'), (1, 12, '2022010002'), (1, 13, '2022010003'),
  (1, 14, '2022010004'), (1, 15, '2022010005'), (1, 16, '2022010006'),
  (1, 24, '2022010007'), (1, 25, '2022010008');

-- class_room 2 (CS101-2022B): students 17,18,19,20
INSERT IGNORE INTO class_student (class_id, student_id, student_no)
VALUES
  (2, 17, '2022020001'), (2, 18, '2022020002'),
  (2, 19, '2022020003'), (2, 20, '2022020004');

-- class_room 3 (CS201-2022A): students 3,12,13,14,15
INSERT IGNORE INTO class_student (class_id, student_id, student_no)
VALUES
  (3, 3,  '2022010001'), (3, 12, '2022010002'), (3, 13, '2022010003'),
  (3, 14, '2022010004'), (3, 15, '2022010005');

-- class_room 4 (CS301-2022A): students 3,12,13,14,15,16,24,25
INSERT IGNORE INTO class_student (class_id, student_id, student_no)
VALUES
  (4, 3,  '2022010001'), (4, 12, '2022010002'), (4, 13, '2022010003'),
  (4, 14, '2022010004'), (4, 15, '2022010005'), (4, 16, '2022010006'),
  (4, 24, '2022010007'), (4, 25, '2022010008');

-- class_room 5 (CS401-2022A): same as class 1
INSERT IGNORE INTO class_student (class_id, student_id, student_no)
VALUES
  (5, 3,  '2022010001'), (5, 12, '2022010002'), (5, 13, '2022010003'),
  (5, 14, '2022010004'), (5, 15, '2022010005'), (5, 16, '2022010006'),
  (5, 24, '2022010007'), (5, 25, '2022010008');

-- class_room 6 (EE101-2022A): students 21,22,23
INSERT IGNORE INTO class_student (class_id, student_id, student_no)
VALUES
  (6, 21, '2022030001'), (6, 22, '2022030002'), (6, 23, '2022030003');

-- 更新班级学生人数
UPDATE class_room SET student_count = 8 WHERE id IN (1,4,5);
UPDATE class_room SET student_count = 4 WHERE id = 2;
UPDATE class_room SET student_count = 5 WHERE id = 3;
UPDATE class_room SET student_count = 3 WHERE id = 6;

-- ─────────────────────────────────────────────────────────────
-- 6. 成绩规则
-- ─────────────────────────────────────────────────────────────
INSERT IGNORE INTO grade_rule (class_id, attend_weight, quiz_weight, interaction_weight, exam_weight, offline_weight)
VALUES
  (1, 20.00, 15.00, 10.00, 45.00, 10.00),
  (2, 20.00, 15.00, 10.00, 45.00, 10.00),
  (3, 15.00, 15.00, 10.00, 50.00, 10.00),
  (4, 15.00, 15.00, 10.00, 50.00, 10.00),
  (5, 20.00, 20.00, 10.00, 40.00, 10.00),
  (6, 25.00, 15.00, 10.00, 40.00, 10.00);

-- ─────────────────────────────────────────────────────────────
-- 7. 课堂
-- status: 0=待开始 1=进行中 2=已结束
-- live_mode: SLIDE_ONLY=普通课堂 ONLINE_CLASS=网课
-- ─────────────────────────────────────────────────────────────
INSERT IGNORE INTO lesson (id, class_id, teacher_id, title, status, start_time, end_time, live_mode, chapter, current_slide)
VALUES
  -- CS101-2022A 已完成课堂
  (1,  1, 2, 'Python入门：变量与数据类型',     2, '2026-03-03 08:00:00', '2026-03-03 09:40:00', 'SLIDE_ONLY', '第1章 Python基础', 12),
  (2,  1, 2, 'Python流程控制：条件与循环',     2, '2026-03-10 08:00:00', '2026-03-10 09:40:00', 'SLIDE_ONLY', '第2章 流程控制', 15),
  (3,  1, 2, 'Python函数与模块',              2, '2026-03-17 08:00:00', '2026-03-17 09:40:00', 'SLIDE_ONLY', '第3章 函数', 18),
  (4,  1, 2, 'Python列表、字典与集合',         2, '2026-03-24 08:00:00', '2026-03-24 09:40:00', 'SLIDE_ONLY', '第4章 容器类型', 20),
  (5,  1, 2, 'Python面向对象编程',             2, '2026-03-31 08:00:00', '2026-03-31 09:40:00', 'SLIDE_ONLY', '第5章 OOP', 22),
  (6,  1, 2, 'Python文件操作与异常处理',       2, '2026-04-07 08:00:00', '2026-04-07 09:40:00', 'SLIDE_ONLY', '第6章 文件与异常', 14),
  (7,  1, 2, 'Python网络爬虫基础',             2, '2026-04-14 08:00:00', '2026-04-14 09:40:00', 'SLIDE_ONLY', '第7章 网络爬虫', 16),
  (8,  1, 2, 'Python数据分析：Pandas入门',     2, '2026-04-21 08:00:00', '2026-04-21 09:40:00', 'SLIDE_ONLY', '第8章 数据分析', 18),
  -- 进行中课堂（当天）
  (9,  1, 2, 'Python可视化：Matplotlib',       1, '2026-06-19 08:00:00', NULL, 'SLIDE_ONLY', '第9章 数据可视化', 5),
  -- 待开始
  (10, 1, 2, 'Python项目实战：学生管理系统',   0, '2026-06-26 08:00:00', NULL, 'SLIDE_ONLY', '第10章 综合项目', 1),
  -- CS201-2022A
  (11, 3, 4, 'Java基础：环境搭建与Hello World', 2, '2026-03-05 10:00:00', '2026-03-05 11:40:00', 'SLIDE_ONLY', '第1章 Java入门', 10),
  (12, 3, 4, 'Java面向对象：类与对象',          2, '2026-03-12 10:00:00', '2026-03-12 11:40:00', 'SLIDE_ONLY', '第2章 OOP基础', 14),
  (13, 3, 4, 'Java集合框架',                   2, '2026-03-19 10:00:00', '2026-03-19 11:40:00', 'SLIDE_ONLY', '第3章 集合框架', 16),
  (14, 3, 4, 'Java异常处理与IO',               2, '2026-03-26 10:00:00', '2026-03-26 11:40:00', 'SLIDE_ONLY', '第4章 异常与IO', 18),
  (15, 3, 4, 'Java多线程编程',                 1, '2026-06-19 10:00:00', NULL, 'SLIDE_ONLY', '第5章 多线程', 7),
  -- CS301-2022A
  (16, 4, 6, '线性表：数组与链表',             2, '2026-03-04 14:00:00', '2026-03-04 15:40:00', 'SLIDE_ONLY', '第1章 线性表', 15),
  (17, 4, 6, '栈与队列',                       2, '2026-03-11 14:00:00', '2026-03-11 15:40:00', 'SLIDE_ONLY', '第2章 栈与队列', 12),
  (18, 4, 6, '树与二叉树',                     2, '2026-03-18 14:00:00', '2026-03-18 15:40:00', 'SLIDE_ONLY', '第3章 树结构', 20),
  (19, 4, 6, '排序算法',                       1, '2026-06-19 14:00:00', NULL, 'SLIDE_ONLY', '第4章 排序', 3);

-- ─────────────────────────────────────────────────────────────
-- 8. 签到记录（attendance_code + attendance）
-- ─────────────────────────────────────────────────────────────
-- 课堂1的签到码（已过期，仅历史记录）
INSERT IGNORE INTO attendance_code (lesson_id, code, qr_token, expire_at)
VALUES
  (1, 'AB123456', 'token_lesson1_qr', '2026-03-03 08:15:00'),
  (2, 'CD234567', 'token_lesson2_qr', '2026-03-10 08:15:00'),
  (3, 'EF345678', 'token_lesson3_qr', '2026-03-17 08:15:00');

-- 签到记录（课堂1，学生全勤）
INSERT IGNORE INTO attendance (lesson_id, class_id, student_id, attended_at, method)
SELECT 1, 1, s.student_id, '2026-03-03 08:05:00', 'QR'
FROM class_student s WHERE s.class_id = 1;

-- 课堂2签到（缺席2人）
INSERT IGNORE INTO attendance (lesson_id, class_id, student_id, attended_at, method)
SELECT 2, 1, s.student_id, '2026-03-10 08:07:00', 'QR'
FROM class_student s WHERE s.class_id = 1 AND s.student_id NOT IN (15, 16);

-- 课堂3-8 全勤
INSERT IGNORE INTO attendance (lesson_id, class_id, student_id, attended_at, method)
SELECT l.id, l.class_id, s.student_id,
       DATE_ADD(l.start_time, INTERVAL FLOOR(2 + RAND()*8) MINUTE), 'QR'
FROM lesson l JOIN class_student s ON s.class_id = l.class_id
WHERE l.id BETWEEN 3 AND 8 AND l.class_id = 1;

-- ─────────────────────────────────────────────────────────────
-- 9. 题库与题目
-- type: 1=单选 2=多选 3=判断 4=填空 5=简答
-- ─────────────────────────────────────────────────────────────
INSERT IGNORE INTO question_bank (id, teacher_id, dept_id, bank_name, description, is_public)
VALUES
  (1, 2, 2, 'Python基础题库',   'Python程序设计课程题目', 1),
  (2, 4, 2, 'Java基础题库',    'Java面向对象课程题目',   1),
  (3, 6, 5, '数据结构题库',    '数据结构与算法题目',     1);

-- Python题库题目（单选题）
INSERT IGNORE INTO question (id, bank_id, type, content, answer, analysis, score, difficulty, creator_id)
VALUES
  (1,  1, 1, 'Python中，以下哪个不是Python的内置数据类型？', 'C', 'Python内置类型包括int/float/str/list/dict/set/bool/tuple等，Array不是内置类型', 5.00, 2, 2),
  (2,  1, 1, '执行print(type(3.0))的输出结果是？', 'B', 'type()函数返回对象的类型，3.0是浮点数', 5.00, 1, 2),
  (3,  1, 1, '下列关于Python列表的说法，错误的是？', 'D', 'Python列表是有序的可变序列，可以包含不同类型元素', 5.00, 2, 2),
  (4,  1, 1, 'Python中用于定义函数的关键字是？', 'A', 'def是Python中定义函数的关键字', 5.00, 1, 2),
  (5,  1, 1, '以下代码的输出结果是：x=5; y=2; print(x//y)', 'B', '//是整除运算符，5//2=2', 5.00, 2, 2),
  (6,  1, 3, 'Python中，列表和元组都是可变数据类型', 'B', '列表(list)可变，元组(tuple)不可变', 5.00, 1, 2),
  (7,  1, 3, 'Python的range(5)生成包含5的序列', 'B', 'range(5)生成0,1,2,3,4，不包含5', 5.00, 1, 2),
  (8,  1, 5, '请简述Python中*args和**kwargs的区别及用途。', NULL, '*args接收位置参数元组，**kwargs接收关键字参数字典', 15.00, 3, 2),
  -- Java题库
  (9,  2, 1, 'Java中，以下哪个关键字用于实现接口？', 'B', 'implements用于实现接口，extends用于继承类', 5.00, 1, 4),
  (10, 2, 1, 'Java中String类是否可以被继承？', 'B', 'String类被final修饰，不可被继承', 5.00, 2, 4),
  (11, 2, 1, '下面哪种集合类型是线程安全的？', 'C', 'Vector是线程安全的，ArrayList和LinkedList不是', 5.00, 3, 4),
  (12, 2, 1, 'Java中，==和equals()的区别，对于String对象，以下说法正确的是？', 'A', '==比较引用地址，equals()比较内容', 5.00, 2, 4),
  (13, 2, 3, 'Java中，int类型占4个字节，取值范围是-2^31到2^31-1', 'A', 'Java int占4字节，范围-2147483648到2147483647', 5.00, 1, 4),
  -- 数据结构题库
  (14, 3, 1, '以下哪种排序算法的平均时间复杂度为O(n log n)？', 'C', '快速排序、归并排序、堆排序平均时间复杂度均为O(n log n)', 5.00, 2, 6),
  (15, 3, 1, '栈的数据操作特点是？', 'A', '栈是后进先出(LIFO)的数据结构', 5.00, 1, 6),
  (16, 3, 2, '以下哪些是二叉搜索树的特点？', 'AB', '左子树所有节点小于根，右子树所有节点大于根，中序遍历有序', 10.00, 2, 6),
  (17, 3, 5, '请描述冒泡排序的实现思路，并分析其时间复杂度。', NULL, '相邻元素两两比较交换，最坏O(n²)，最好O(n)，平均O(n²)', 20.00, 2, 6);

-- 题目选项
INSERT IGNORE INTO question_option (question_id, option_label, content, is_correct, sort_order)
VALUES
  -- Q1 Python内置类型
  (1, 'A', 'int', 0, 1), (1, 'B', 'float', 0, 2), (1, 'C', 'Array', 1, 3), (1, 'D', 'dict', 0, 4),
  -- Q2 type(3.0)
  (2, 'A', "<class 'int'>", 0, 1), (2, 'B', "<class 'float'>", 1, 2), (2, 'C', "<class 'str'>", 0, 3), (2, 'D', "<class 'number'>", 0, 4),
  -- Q3 列表错误说法
  (3, 'A', '列表可以包含不同类型的元素', 0, 1),
  (3, 'B', '列表是有序的', 0, 2),
  (3, 'C', '列表可以通过索引访问元素', 0, 3),
  (3, 'D', '列表的长度是固定的', 1, 4),
  -- Q4 def关键字
  (4, 'A', 'def', 1, 1), (4, 'B', 'function', 0, 2), (4, 'C', 'func', 0, 3), (4, 'D', 'define', 0, 4),
  -- Q5 整除
  (5, 'A', '1', 0, 1), (5, 'B', '2', 1, 2), (5, 'C', '2.5', 0, 3), (5, 'D', '3', 0, 4),
  -- Q9 implements
  (9, 'A', 'extends', 0, 1), (9, 'B', 'implements', 1, 2), (9, 'C', 'interface', 0, 3), (9, 'D', 'override', 0, 4),
  -- Q10 String继承
  (10, 'A', '可以', 0, 1), (10, 'B', '不可以，因为String是final类', 1, 2), (10, 'C', '可以，但要遵守规范', 0, 3), (10, 'D', '只能通过内部类继承', 0, 4),
  -- Q11 线程安全集合
  (11, 'A', 'ArrayList', 0, 1), (11, 'B', 'LinkedList', 0, 2), (11, 'C', 'Vector', 1, 3), (11, 'D', 'HashMap', 0, 4),
  -- Q12 ==和equals
  (12, 'A', '==比较引用，equals()比较内容', 1, 1),
  (12, 'B', '==和equals()都比较内容', 0, 2),
  (12, 'C', '==和equals()都比较引用', 0, 3),
  (12, 'D', '==比较内容，equals()比较引用', 0, 4),
  -- Q14 排序时间复杂度
  (14, 'A', '冒泡排序', 0, 1), (14, 'B', '插入排序', 0, 2), (14, 'C', '快速排序', 1, 3), (14, 'D', '选择排序', 0, 4),
  -- Q15 栈特点
  (15, 'A', '后进先出（LIFO）', 1, 1), (15, 'B', '先进先出（FIFO）', 0, 2), (15, 'C', '随机访问', 0, 3), (15, 'D', '优先级访问', 0, 4),
  -- Q16 BST特点（多选）
  (16, 'A', '左子树所有节点值小于根节点', 1, 1),
  (16, 'B', '中序遍历结果为有序序列', 1, 2),
  (16, 'C', '查找、插入、删除时间复杂度均为O(1)', 0, 3),
  (16, 'D', '右子树所有节点值小于根节点', 0, 4);

-- ─────────────────────────────────────────────────────────────
-- 10. 试卷
-- ─────────────────────────────────────────────────────────────
INSERT IGNORE INTO exam_paper (id, creator_id, title, total_score, is_random, paper_type, description)
VALUES
  (1, 2, 'Python基础期中测试', 100.00, 0, 'A', 'Python程序设计基础期中考试试卷'),
  (2, 2, 'Python基础期末考试', 100.00, 0, 'A', 'Python程序设计基础期末考试试卷'),
  (3, 4, 'Java基础阶段测试',   100.00, 0, 'A', 'Java面向对象程序设计阶段测试');

-- 试卷题目关联
INSERT IGNORE INTO exam_paper_question (paper_id, question_id, score, sort_order, paper_group, section)
VALUES
  -- Paper 1: Python期中（单选35+判断10+简答15 = 60，加上其他略过简化到100）
  (1, 1, 5.00,  1, 'A', '一、单选题'),
  (1, 2, 5.00,  2, 'A', '一、单选题'),
  (1, 3, 5.00,  3, 'A', '一、单选题'),
  (1, 4, 5.00,  4, 'A', '一、单选题'),
  (1, 5, 5.00,  5, 'A', '一、单选题'),
  (1, 6, 5.00,  6, 'A', '二、判断题'),
  (1, 7, 5.00,  7, 'A', '二、判断题'),
  (1, 8, 70.00, 8, 'A', '三、简答题'),
  -- Paper 2: Python期末（相同结构）
  (2, 1, 5.00,  1, 'A', '一、单选题'),
  (2, 2, 5.00,  2, 'A', '一、单选题'),
  (2, 3, 5.00,  3, 'A', '一、单选题'),
  (2, 4, 5.00,  4, 'A', '一、单选题'),
  (2, 5, 5.00,  5, 'A', '一、单选题'),
  (2, 6, 5.00,  6, 'A', '二、判断题'),
  (2, 7, 5.00,  7, 'A', '二、判断题'),
  (2, 8, 70.00, 8, 'A', '三、简答题'),
  -- Paper 3: Java阶段测试
  (3, 9,  5.00, 1, 'A', '一、单选题'),
  (3, 10, 5.00, 2, 'A', '一、单选题'),
  (3, 11, 5.00, 3, 'A', '一、单选题'),
  (3, 12, 5.00, 4, 'A', '一、单选题'),
  (3, 13, 5.00, 5, 'A', '二、判断题'),
  (3, 8,  75.00,6, 'A', '三、简答题');

-- ─────────────────────────────────────────────────────────────
-- 11. 发布考试（期中考试已结束）
-- status: 0=未开始 1=进行中 2=已结束
-- ─────────────────────────────────────────────────────────────
INSERT IGNORE INTO exam_publish (id, paper_id, class_id, teacher_id, start_time, end_time, duration_min, enable_monitor, face_verify_type, status)
VALUES
  (1, 1, 1, 2, '2026-04-28 08:00:00', '2026-04-28 10:00:00', 120, 0, 0, 2),
  (2, 3, 3, 4, '2026-04-30 10:00:00', '2026-04-30 11:30:00',  90, 0, 0, 2);

-- ─────────────────────────────────────────────────────────────
-- 12. 学生答题与成绩（期中考试已阅卷）
-- ─────────────────────────────────────────────────────────────
-- student01 答题（publish_id=1）
INSERT IGNORE INTO student_answer (publish_id, student_id, question_id, answer_content, score, is_correct, review_status, submitted_at)
VALUES
  (1, 3, 1, 'C', 5.00,  1, 1, '2026-04-28 08:45:00'),
  (1, 3, 2, 'B', 5.00,  1, 1, '2026-04-28 08:46:00'),
  (1, 3, 3, 'D', 5.00,  1, 1, '2026-04-28 08:47:00'),
  (1, 3, 4, 'A', 5.00,  1, 1, '2026-04-28 08:48:00'),
  (1, 3, 5, 'B', 5.00,  1, 1, '2026-04-28 08:49:00'),
  (1, 3, 6, 'B', 5.00,  1, 1, '2026-04-28 08:50:00'),
  (1, 3, 7, 'B', 5.00,  1, 1, '2026-04-28 08:51:00'),
  (1, 3, 8, '*args接收任意数量的位置参数，以元组形式传递；**kwargs接收任意数量的关键字参数，以字典形式传递。', 60.00, NULL, 2, '2026-04-28 09:30:00');

-- student02 答题（略有错误）
INSERT IGNORE INTO student_answer (publish_id, student_id, question_id, answer_content, score, is_correct, review_status, submitted_at)
VALUES
  (1, 12, 1, 'C', 5.00, 1, 1, '2026-04-28 08:50:00'),
  (1, 12, 2, 'A', 0.00, 0, 1, '2026-04-28 08:51:00'),
  (1, 12, 3, 'D', 5.00, 1, 1, '2026-04-28 08:52:00'),
  (1, 12, 4, 'A', 5.00, 1, 1, '2026-04-28 08:53:00'),
  (1, 12, 5, 'B', 5.00, 1, 1, '2026-04-28 08:54:00'),
  (1, 12, 6, 'A', 0.00, 0, 1, '2026-04-28 08:55:00'),
  (1, 12, 7, 'B', 5.00, 1, 1, '2026-04-28 08:56:00'),
  (1, 12, 8, '*args和**kwargs都用于接受可变参数。', 45.00, NULL, 2, '2026-04-28 09:40:00');

-- 成绩汇总（student_grade: class_id + student_id）
INSERT IGNORE INTO student_grade (class_id, student_id, exam_score, attend_score, interaction_score, total_score, calc_status)
VALUES
  (1, 3,  95.00, 18.00, 8.00, 92.50, 1),
  (1, 12, 70.00, 15.00, 7.00, 72.80, 1),
  (1, 13, 85.00, 16.00, 8.00, 83.40, 1),
  (1, 14, 88.00, 17.00, 9.00, 86.90, 1),
  (1, 15, 72.00, 14.00, 6.00, 72.00, 1),
  (1, 16, 90.00, 18.00, 9.00, 88.80, 1),
  (1, 24, 78.00, 16.00, 7.00, 77.60, 1),
  (1, 25, 92.00, 17.00, 8.00, 89.40, 1);

-- ─────────────────────────────────────────────────────────────
-- 13. 弹幕样本数据
-- ─────────────────────────────────────────────────────────────
INSERT IGNORE INTO barrage (lesson_id, student_id, content, style, created_at)
VALUES
  (1, 3,  '老师讲得很清楚！',              'roll', '2026-03-03 08:20:00'),
  (1, 12, '变量命名有规范吗？',            'roll', '2026-03-03 08:25:00'),
  (1, 13, '这个例子很好理解',              'roll', '2026-03-03 08:30:00'),
  (2, 3,  'for循环和while有什么区别？',   'roll', '2026-03-10 08:22:00'),
  (2, 14, '明白了！',                      'roll', '2026-03-10 08:35:00'),
  (9, 3,  'Matplotlib和Seaborn哪个好用？','roll', '2026-06-19 08:10:00'),
  (9, 12, '这个图例怎么设置？',            'roll', '2026-06-19 08:15:00');

-- ─────────────────────────────────────────────────────────────
-- 完成提示
-- ─────────────────────────────────────────────────────────────
SELECT '=== 测试数据注入完成 ===' AS status;
SELECT CONCAT('用户数: ', COUNT(*)) AS info FROM sys_user;
SELECT CONCAT('课程数: ', COUNT(*)) AS info FROM course;
SELECT CONCAT('班级数: ', COUNT(*)) AS info FROM class_room;
SELECT CONCAT('课堂数: ', COUNT(*)) AS info FROM lesson;
SELECT CONCAT('题目数: ', COUNT(*)) AS info FROM question;
