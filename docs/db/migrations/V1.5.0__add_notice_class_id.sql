-- S8-10：通知公告支持 CLASS（指定班级）范围推送，补 class_id 列。
-- scope=CLASS 时有效，指向 class_room.id；SCHOOL/DEPT 时为 NULL。
ALTER TABLE notice
    ADD COLUMN class_id BIGINT NULL COMMENT '指定班级ID（scope=CLASS时有效）→ class_room.id' AFTER dept_id,
    ADD INDEX idx_class_id (class_id);
