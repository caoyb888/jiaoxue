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

    public void saveAiContent(Long lessonId, String summary, String mindmapJson) {
        reportMapper.updateAiContent(lessonId, summary, mindmapJson, 2);
        log.info("AI内容已写入课堂报告: lessonId={}", lessonId);
    }

    public void markFailed(Long lessonId) {
        reportMapper.updateAiContent(lessonId, null, null, 3);
    }
}
