package cn.smu.edu.stat.domain.vo;

/**
 * 院系某个时间桶（day/week/month）的教学统计，由 ClickHouse {@code lesson_event_log}
 * 明细按时间桶聚合。
 *
 * @param periodStart        时间桶起始日期（yyyy-MM-dd）：day=当天，week=周一，month=月初
 * @param lessonCount        桶内开课数（去重 lesson_id）
 * @param classCount         桶内活跃班级数（去重 class_id）
 * @param attendCount        签到事件量（ATTEND 桶）
 * @param barrageCount       弹幕事件量（BARRAGE 桶）
 * @param questionCount      课堂提问/答题事件量（QUESTION 桶）
 * @param scoreCount         加分事件量（SCORE 桶）
 * @param slideCount         翻页事件量（SLIDE 桶）
 * @param activeStudentCount 去重活跃学生数（student_id > 0）
 */
public record DeptPeriodStatVO(
        String periodStart,
        long lessonCount,
        long classCount,
        long attendCount,
        long barrageCount,
        long questionCount,
        long scoreCount,
        long slideCount,
        long activeStudentCount) {
}
