package cn.smu.edu.stat.domain.vo;

import java.util.Map;

/**
 * 单个课堂实时统计（最近 {@code windowMinutes} 分钟滑动窗口聚合自 Redis）。
 *
 * @param lessonId           课堂 ID
 * @param windowMinutes      统计窗口（分钟），固定 5
 * @param onlineStudentCount 窗口内该课堂活跃的去重学生数
 * @param eventVolume        该课堂各事件桶在窗口内的发生量
 */
public record LessonRealtimeVO(
        Long lessonId,
        int windowMinutes,
        long onlineStudentCount,
        Map<String, Long> eventVolume) {
}
