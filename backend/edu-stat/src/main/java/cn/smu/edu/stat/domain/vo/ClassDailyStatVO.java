package cn.smu.edu.stat.domain.vo;

/**
 * 班级某一天的教学统计（由 ClickHouse {@code lesson_event_log} 明细按天聚合）。
 *
 * @param statDate           统计日期（yyyy-MM-dd）
 * @param lessonCount        当天开课数（去重 lesson_id）
 * @param attendCount        签到事件量（ATTEND 桶）
 * @param barrageCount       弹幕事件量（BARRAGE 桶）
 * @param questionCount      课堂提问/答题事件量（QUESTION 桶）
 * @param scoreCount         加分事件量（SCORE 桶）
 * @param slideCount         翻页事件量（SLIDE 桶）
 * @param activeStudentCount 去重活跃学生数（student_id > 0）
 */
public record ClassDailyStatVO(
        String statDate,
        long lessonCount,
        long attendCount,
        long barrageCount,
        long questionCount,
        long scoreCount,
        long slideCount,
        long activeStudentCount) {
}
