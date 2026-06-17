package cn.smu.edu.interaction.service;

import cn.smu.edu.interaction.domain.entity.LessonReport;

public interface LessonReportService {

    /**
     * 课堂结束时初始化报告（统计字段落库，gen_status=0-待生成）
     */
    LessonReport initReport(Long lessonId, int durationMin);

    /**
     * 更新 AI 内容（gen_status=2-完成）
     */
    void saveAiContent(Long lessonId, String aiSummary, String mindmapJson);

    /**
     * 标记 AI 生成失败（gen_status=3-失败）
     */
    void markFailed(Long lessonId);

    LessonReport getByLessonId(Long lessonId);
}
