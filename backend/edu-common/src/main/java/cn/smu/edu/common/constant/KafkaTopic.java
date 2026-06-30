package cn.smu.edu.common.constant;

/**
 * Kafka Topic 常量 — 命名规范: edu.{domain}.{action}
 */
public final class KafkaTopic {

    public static final String ATTEND_EVENTS    = "edu.attend.events";    // 签到事件
    public static final String AI_TASKS         = "edu.ai.tasks";         // AI异步任务（concurrency=3）
    public static final String EXAM_SUBMIT      = "edu.exam.submit";      // 交卷事件（concurrency=10）
    public static final String TEACHING_EVENTS  = "edu.teaching.events";  // 课堂事件（concurrency=5）
    public static final String NOTICE           = "edu.notice";            // 通知推送
    public static final String NOTICE_PUBLISH   = "edu.notice.publish";    // 通知公告发布（→批量微信订阅推送）
    public static final String AUDIT_LOG        = "edu.audit.log";        // 操作审计日志
    public static final String STAT_EVENTS      = "edu.stat.events";      // 统计事件→ClickHouse
    public static final String MATERIAL_CONVERT = "edu.material.convert";  // 课件转换（LibreOffice → 图片序列）
    public static final String DISCUSSION_EVENTS = "edu.discussion.events"; // 分组讨论消息（→edu-ai 汇总）

    private KafkaTopic() {}
}
