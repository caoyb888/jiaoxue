package cn.smu.edu.stat.domain.vo;

/**
 * 教学预警事件视图（S7-07），供管理员预警列表页（S7-14）展示。
 *
 * @param id             预警 ID
 * @param warnType       预警类型 LOW_ATTEND/ZERO_ACTIVE/FREQUENT_ABSENCE
 * @param targetType     对象类型 LESSON/STUDENT
 * @param targetId       对象 ID
 * @param lessonId       关联课堂 ID（可空）
 * @param classId        关联班级 ID（可空）
 * @param deptId         关联院系 ID（可空）
 * @param teacherId      关联教师 ID（可空）
 * @param statDate       统计日期（yyyy-MM-dd）
 * @param metricValue    触发指标实际值
 * @param thresholdValue 阈值
 * @param detail         预警详情（人类可读）
 * @param status         处理状态 0未处理/1已处理/2忽略
 * @param createdAt      生成时间（ISO 字符串）
 */
public record WarnVO(
        Long id,
        String warnType,
        String targetType,
        Long targetId,
        Long lessonId,
        Long classId,
        Long deptId,
        Long teacherId,
        String statDate,
        Integer metricValue,
        Integer thresholdValue,
        String detail,
        Integer status,
        String createdAt) {
}
