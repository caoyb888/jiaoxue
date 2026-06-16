package cn.smu.edu.common.constant;

/**
 * Redis Key 常量 — 格式: {service}:{domain}:{identifier}[:{field}]
 */
public final class RedisKey {

    // 签到相关
    public static final String ATTEND_QUEUE = "attend:queue:%d";          // List  签到队列
    public static final String ATTEND_BLOOM = "attend:bloom:%d";          // 布隆过滤器
    public static final String ATTEND_COUNT = "attend:count:%d";          // String/incr 签到计数
    public static final String ATTEND_CODE  = "attend:code:%d";           // 签到码

    // 考试相关
    public static final String EXAM_SUBMIT_IDEM  = "exam:submit:%d:%d";  // 交卷幂等键 TTL 30min
    public static final String EXAM_ANSWER_CACHE = "exam:answer:%d:%d";  // 答案暂存 TTL 2h

    // AI 相关
    public static final String AI_TASK_DONE = "ai:task:done:%s";         // 任务去重键 TTL 24h

    // 用户会话
    public static final String SESSION = "session:%s";                    // Hash TTL 2h
    public static final String SMS_CODE = "sms:code:%s";                 // String TTL 5min

    // 统计缓存
    public static final String STAT_REALTIME = "stat:realtime:overview";  // TTL 5min

    public static String format(String template, Object... args) {
        return String.format(template, args);
    }

    private RedisKey() {}
}
