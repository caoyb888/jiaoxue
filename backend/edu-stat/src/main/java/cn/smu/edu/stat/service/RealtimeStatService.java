package cn.smu.edu.stat.service;

import cn.smu.edu.common.event.TeachingEvent;
import cn.smu.edu.stat.domain.vo.LessonRealtimeVO;
import cn.smu.edu.stat.domain.vo.RealtimeOverviewVO;

import java.util.List;

/**
 * 实时统计聚合（S7-02）。
 *
 * <p>把课堂事件实时聚合进 Redis（db=7）的 5 分钟滑动窗口，供大屏/课堂实时 API（S7-03）读取。
 * 与 ClickHouse 明细层（S7-01）相互独立：ClickHouse 留长期明细做历史分析，Redis 只保留
 * 最近 5 分钟的"当前态"，键 TTL 5min 且每次事件刷新，活动停止后自动过期归零。
 */
public interface RealtimeStatService {

    /** ClickHouse / 实时统计共用的事件桶集合（与 {@code LessonEventConsumer} 归一映射一致）。 */
    List<String> EVENT_BUCKETS = List.of("ATTEND", "BARRAGE", "QUESTION", "SCORE", "SLIDE");

    /** 滑动窗口长度（分钟）。 */
    int WINDOW_MINUTES = 5;

    /**
     * 记录一条课堂事件到实时聚合窗口。
     *
     * @param event  原始课堂事件（取 lessonId / payload.studentId）
     * @param bucket 归一后的事件桶（{@link #EVENT_BUCKETS} 之一）
     */
    void record(TeachingEvent event, String bucket);

    /** 全局实时概览（活跃课堂数、在线人数、各事件桶发生量）。 */
    RealtimeOverviewVO overview();

    /** 指定课堂的实时统计。 */
    LessonRealtimeVO lessonRealtime(Long lessonId);
}
