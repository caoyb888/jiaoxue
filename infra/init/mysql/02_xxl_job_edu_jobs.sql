--
-- XXL-Job 执行器分组与定时任务注册（edu-exam）
-- 依赖 01_xxl_job_schema.sql 已建表。幂等：可重复执行。
-- 对应 edu-exam 中的 @XxlJob handler：
--   examSubmitExpandHandler  C2 第三层：每30s 展开 exam_submit_queue → student_answer + 客观题自动批改
--   examStatusSyncHandler    每分钟同步 exam_publish.status 冗余字段
--   heartbeatTimeoutHandler  每分钟检测监考心跳超时（>90s 标记 OFFLINE）
--

USE `xxl_job`;
SET NAMES utf8mb4;

-- edu-exam 执行器分组（address_type=0：执行器启动后自动上报地址）
INSERT INTO `xxl_job_group`(`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`)
VALUES (2, 'edu-exam', '考试服务执行器', 0, NULL, NOW())
ON DUPLICATE KEY UPDATE `app_name`=VALUES(`app_name`), `title`=VALUES(`title`), `update_time`=NOW();

-- 三个定时任务（trigger_status=1 启用；trigger_next_time=0 由调度器首扫按 CRON 自动重算）
INSERT INTO `xxl_job_info`
(`id`,`job_group`,`job_desc`,`add_time`,`update_time`,`author`,`alarm_email`,`schedule_type`,`schedule_conf`,`misfire_strategy`,`executor_route_strategy`,`executor_handler`,`executor_param`,`executor_block_strategy`,`executor_timeout`,`executor_fail_retry_count`,`glue_type`,`glue_source`,`glue_remark`,`glue_updatetime`,`child_jobid`,`trigger_status`)
VALUES
(2,2,'交卷队列展开+客观题批改(C2第三层)',NOW(),NOW(),'system','','CRON','0/30 * * * * ?','DO_NOTHING','FIRST','examSubmitExpandHandler','','SERIAL_EXECUTION',0,0,'BEAN','','',NOW(),'',1),
(3,2,'考试发布状态同步(每分钟)',NOW(),NOW(),'system','','CRON','0 * * * * ?','DO_NOTHING','FIRST','examStatusSyncHandler','','SERIAL_EXECUTION',0,0,'BEAN','','',NOW(),'',1),
(4,2,'监考心跳超时检测(每分钟)',NOW(),NOW(),'system','','CRON','0 * * * * ?','DO_NOTHING','FIRST','heartbeatTimeoutHandler','','SERIAL_EXECUTION',0,0,'BEAN','','',NOW(),'',1)
ON DUPLICATE KEY UPDATE `executor_handler`=VALUES(`executor_handler`),`schedule_conf`=VALUES(`schedule_conf`),`job_group`=VALUES(`job_group`),`trigger_status`=VALUES(`trigger_status`),`update_time`=NOW();

commit;
