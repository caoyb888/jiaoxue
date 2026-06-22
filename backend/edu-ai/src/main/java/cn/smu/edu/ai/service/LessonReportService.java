package cn.smu.edu.ai.service;

import cn.smu.edu.ai.domain.entity.LessonReport;
import cn.smu.edu.ai.repository.LessonReportMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonReportService {

    private final LessonReportMapper reportMapper;

    public LessonReport getByLessonId(Long lessonId) {
        return reportMapper.selectByLessonId(lessonId);
    }

    public void initReport(Long lessonId, int durationMin) {
        boolean exists = reportMapper.selectCount(
                new LambdaQueryWrapper<LessonReport>().eq(LessonReport::getLessonId, lessonId)) > 0;
        if (exists) return;

        LessonReport report = LessonReport.builder()
                .lessonId(lessonId)
                .durationMin(durationMin)
                .genStatus(1)
                .attendCount(0)
                .absentCount(0)
                .interactCount(0)
                .quizCount(0)
                .slideCount(0)
                .mindmapVisible(0)
                .summaryVisible(0)
                .build();
        reportMapper.insert(report);
        log.info("课堂报告记录已创建: lessonId={}", lessonId);
    }

    /** SUMMARY 任务：一次性写入摘要 + 关键点 + 思维导图，gen_status=2 */
    public void saveAiContent(Long lessonId, String summary, String keyPointsJson, String mindmapJson) {
        reportMapper.updateAiContent(lessonId, summary, keyPointsJson, mindmapJson, 2);
        log.info("AI内容已写入课堂报告: lessonId={}", lessonId);
    }

    /** MINDMAP 任务：仅写思维导图，不覆盖已有摘要/关键点 */
    public void saveMindmap(Long lessonId, String mindmapJson) {
        reportMapper.updateMindmap(lessonId, mindmapJson, 2);
        log.info("AI思维导图已写入课堂报告: lessonId={}", lessonId);
    }

    public void markFailed(Long lessonId) {
        reportMapper.updateGenStatus(lessonId, 3);
    }

    /** 标记为生成中（重新生成触发时） */
    public void markGenerating(Long lessonId) {
        reportMapper.updateGenStatus(lessonId, 1);
    }

    /** 更新思维导图对学生可见性 */
    public void updateMindmapVisible(Long lessonId, boolean visible) {
        reportMapper.updateMindmapVisible(lessonId, visible ? 1 : 0);
    }
}
