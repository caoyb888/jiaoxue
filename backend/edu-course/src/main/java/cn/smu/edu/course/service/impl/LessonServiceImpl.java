package cn.smu.edu.course.service.impl;

import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.common.event.AiTaskEvent;
import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.course.domain.dto.LessonQueryDTO;
import cn.smu.edu.course.domain.dto.LessonScheduleCreateDTO;
import cn.smu.edu.course.domain.dto.LessonStartDTO;
import cn.smu.edu.course.domain.entity.ClassRoom;
import cn.smu.edu.course.domain.entity.Lesson;
import cn.smu.edu.course.domain.entity.LessonSchedule;
import cn.smu.edu.course.domain.vo.LessonDetailVO;
import cn.smu.edu.course.domain.vo.LessonEndVO;
import cn.smu.edu.course.domain.vo.LessonScheduleVO;
import cn.smu.edu.course.domain.vo.LessonStartVO;
import cn.smu.edu.course.repository.ClassRoomMapper;
import cn.smu.edu.course.repository.LessonMapper;
import cn.smu.edu.course.repository.LessonScheduleMapper;
import cn.smu.edu.course.service.LessonService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final LessonMapper lessonMapper;
    private final LessonScheduleMapper scheduleMapper;
    private final ClassRoomMapper classRoomMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.ws.endpoint:wss://api.smu.edu.cn/ws}")
    private String wsEndpoint;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LessonStartVO startLesson(Long teacherId, LessonStartDTO dto) {
        ClassRoom classRoom = classRoomMapper.selectById(dto.getClassId());
        if (classRoom == null || classRoom.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.CLASS_NOT_FOUND);
        }

        // SLIDE_ONLY mode: no WebRTC/RTMP (C5 constraint)
        String liveMode = "SLIDE_ONLY".equals(dto.getLiveMode()) ? "SLIDE_ONLY"
                : (dto.getLiveMode() != null ? dto.getLiveMode() : "SLIDE_ONLY");

        Lesson lesson = new Lesson();
        lesson.setClassId(dto.getClassId());
        lesson.setTeacherId(teacherId);
        lesson.setMaterialId(dto.getMaterialId());
        lesson.setTitle(dto.getTitle() != null ? dto.getTitle()
                : "第" + (System.currentTimeMillis() % 100) + "次课堂");
        lesson.setChapter(dto.getChapter());
        lesson.setStatus(1);
        lesson.setStartTime(LocalDateTime.now());
        lesson.setLiveMode(liveMode);
        lesson.setCurrentSlide(1);
        lesson.setReplayVisible(1);

        // SLIDE_ONLY: push/play URL must be null (C5)
        if ("SLIDE_ONLY".equals(liveMode)) {
            lesson.setLivePushUrl(null);
            lesson.setLivePlayUrl(null);
        }

        lessonMapper.insert(lesson);
        log.info("课堂开始: lessonId={}, classId={}, teacherId={}, liveMode={}", lesson.getId(), dto.getClassId(), teacherId, liveMode);

        LessonStartVO vo = new LessonStartVO();
        vo.setLessonId(lesson.getId());
        vo.setStatus(1);
        vo.setLiveMode(liveMode);
        vo.setWsEndpoint(wsEndpoint);
        vo.setWsTopicPrefix("/topic/lesson/" + lesson.getId());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LessonEndVO endLesson(Long lessonId, Long teacherId) {
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null || lesson.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.LESSON_NOT_FOUND);
        }
        if (lesson.getStatus() == 2) {
            throw new BizException(ErrorCode.LESSON_ALREADY_ENDED);
        }

        LocalDateTime endTime = LocalDateTime.now();
        lesson.setStatus(2);
        lesson.setEndTime(endTime);
        lessonMapper.updateById(lesson);

        int durationMin = (int) ChronoUnit.MINUTES.between(lesson.getStartTime(), endTime);
        log.info("课堂结束: lessonId={}, teacherId={}, durationMin={}", lessonId, teacherId, durationMin);

        // 异步触发 AI 任务 (C3约束: 课堂结束→Kafka，立即返回)
        String taskId = UUID.randomUUID().toString().replace("-", "");
        AiTaskEvent event = AiTaskEvent.lessonSummary(lessonId, teacherId, lesson.getClassId(), taskId);
        try {
            kafkaTemplate.send(KafkaTopic.AI_TASKS, taskId, event);
            log.info("AI任务已触发: lessonId={}, taskId={}", lessonId, taskId);
        } catch (Exception e) {
            // Kafka 发送失败不阻塞主流程，AI 任务会通过重试机制处理
            log.warn("AI任务Kafka发送失败，不影响课堂结束: lessonId={}, error={}", lessonId, e.getMessage());
        }

        LessonEndVO vo = new LessonEndVO();
        vo.setLessonId(lessonId);
        vo.setStatus(2);
        vo.setDurationMin(durationMin);
        vo.setAiTaskTriggered(true);
        vo.setMessage("课堂已结束，AI报告正在生成中，完成后将通过通知推送给您");
        return vo;
    }

    @Override
    public PageResult<LessonDetailVO> listLessons(LessonQueryDTO query) {
        Page<LessonDetailVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<LessonDetailVO> result = lessonMapper.selectLessonPage(page, query.getClassId(), query.getStatus());
        return PageResult.of(result);
    }

    @Override
    public LessonDetailVO getLessonDetail(Long lessonId) {
        LessonDetailVO vo = lessonMapper.selectLessonDetail(lessonId);
        if (vo == null) {
            throw new BizException(ErrorCode.LESSON_NOT_FOUND);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentSlide(Long lessonId, Integer slideNo, Long teacherId) {
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null || lesson.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.LESSON_NOT_FOUND);
        }
        if (lesson.getStatus() != 1) {
            throw new BizException(ErrorCode.LESSON_NOT_STARTED);
        }
        lesson.setCurrentSlide(slideNo);
        lessonMapper.updateById(lesson);
        log.info("课件翻页: lessonId={}, slide={}, teacherId={}", lessonId, slideNo, teacherId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSchedule(Long teacherId, LessonScheduleCreateDTO dto) {
        LessonSchedule schedule = new LessonSchedule();
        schedule.setClassId(dto.getClassId());
        schedule.setTeacherId(teacherId);
        schedule.setScheduledAt(dto.getScheduledAt());
        schedule.setRepeatType(dto.getRepeatType() != null ? dto.getRepeatType() : "NONE");
        schedule.setRepeatEndAt(dto.getRepeatEndAt());
        schedule.setWeekDay(dto.getWeekDay());
        schedule.setStatus(0);
        scheduleMapper.insert(schedule);
        log.info("排课创建: scheduleId={}, classId={}, scheduledAt={}", schedule.getId(), dto.getClassId(), dto.getScheduledAt());
        return schedule.getId();
    }

    @Override
    public LessonScheduleVO getSchedule(Long scheduleId) {
        LessonSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        LessonScheduleVO vo = new LessonScheduleVO();
        vo.setId(schedule.getId());
        vo.setClassId(schedule.getClassId());
        vo.setScheduledAt(schedule.getScheduledAt());
        vo.setRepeatType(schedule.getRepeatType());
        vo.setRepeatEndAt(schedule.getRepeatEndAt());
        vo.setWeekDay(schedule.getWeekDay());
        vo.setLessonId(schedule.getLessonId());
        vo.setStatus(schedule.getStatus());
        vo.setCreatedAt(schedule.getCreatedAt());
        return vo;
    }
}
