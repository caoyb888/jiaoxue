package cn.smu.edu.stat.domain.vo;

import java.util.Map;

/**
 * 大屏实时概览（最近 {@code windowMinutes} 分钟滑动窗口聚合自 Redis）。
 *
 * @param windowMinutes      统计窗口（分钟），固定 5
 * @param activeLessonCount  窗口内有事件的活跃课堂数（开课数）
 * @param onlineStudentCount 窗口内活跃的去重学生数（在线人数）
 * @param eventVolume        各事件桶在窗口内的发生量（ATTEND/BARRAGE/QUESTION/SCORE/SLIDE）
 */
public record RealtimeOverviewVO(
        int windowMinutes,
        long activeLessonCount,
        long onlineStudentCount,
        Map<String, Long> eventVolume) {
}
