package cn.smu.edu.interaction.service.impl;

import cn.smu.edu.interaction.domain.entity.LessonReport;
import cn.smu.edu.interaction.repository.AttendanceMapper;
import cn.smu.edu.interaction.repository.BarrageMapper;
import cn.smu.edu.interaction.repository.LessonReportMapper;
import cn.smu.edu.interaction.service.LessonReportService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonReportServiceImpl implements LessonReportService {

    private final LessonReportMapper lessonReportMapper;
    private final AttendanceMapper attendanceMapper;
    private final BarrageMapper barrageMapper;

    @Override
    public LessonReport initReport(Long lessonId, int durationMin) {
        // 统计签到数据
        int attendedCount = attendanceMapper.countAttended(lessonId);
        int totalCount = attendanceMapper.selectCount(
                new LambdaQueryWrapper<cn.smu.edu.interaction.domain.entity.Attendance>()
                        .eq(cn.smu.edu.interaction.domain.entity.Attendance::getLessonId, lessonId)).intValue();
        int absentCount = Math.max(0, totalCount - attendedCount);

        BigDecimal attendRate = totalCount > 0
                ? BigDecimal.valueOf(attendedCount * 100.0 / totalCount).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 统计互动数（弹幕数作为互动参考）
        int interactCount = barrageMapper.selectCount(
                new LambdaQueryWrapper<cn.smu.edu.interaction.domain.entity.Barrage>()
                        .eq(cn.smu.edu.interaction.domain.entity.Barrage::getLessonId, lessonId)
                        .eq(cn.smu.edu.interaction.domain.entity.Barrage::getIsBlocked, 0)).intValue();

        // 检查是否已存在报告（幂等）
        LessonReport existing = lessonReportMapper.selectOne(
                new LambdaQueryWrapper<LessonReport>()
                        .eq(LessonReport::getLessonId, lessonId));
        if (existing != null) {
            return existing;
        }

        LessonReport report = new LessonReport();
        report.setLessonId(lessonId);
        report.setAttendCount(attendedCount);
        report.setAbsentCount(absentCount);
        report.setAttendRate(attendRate);
        report.setInteractCount(interactCount);
        report.setQuizCount(0);
        report.setDurationMin(durationMin);
        report.setSlideCount(0);
        report.setMindmapVisible(0);
        report.setSummaryVisible(0);
        report.setGenStatus(0); // 待生成
        lessonReportMapper.insert(report);

        log.info("课堂报告初始化: lessonId={}, attended={}/{}, rate={}%", lessonId, attendedCount, totalCount, attendRate);
        return report;
    }

    @Override
    public void saveAiContent(Long lessonId, String aiSummary, String mindmapJson) {
        lessonReportMapper.update(null, new LambdaUpdateWrapper<LessonReport>()
                .eq(LessonReport::getLessonId, lessonId)
                .set(LessonReport::getAiSummary, aiSummary)
                .set(LessonReport::getAiMindmapJson, mindmapJson)
                .set(LessonReport::getGenStatus, 2));
        log.info("AI 内容保存完成: lessonId={}", lessonId);
    }

    @Override
    public void markFailed(Long lessonId) {
        lessonReportMapper.update(null, new LambdaUpdateWrapper<LessonReport>()
                .eq(LessonReport::getLessonId, lessonId)
                .set(LessonReport::getGenStatus, 3));
    }

    @Override
    public LessonReport getByLessonId(Long lessonId) {
        return lessonReportMapper.selectOne(
                new LambdaQueryWrapper<LessonReport>()
                        .eq(LessonReport::getLessonId, lessonId));
    }
}
