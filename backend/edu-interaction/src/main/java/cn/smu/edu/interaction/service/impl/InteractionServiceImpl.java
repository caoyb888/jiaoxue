package cn.smu.edu.interaction.service.impl;

import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.common.event.TeachingEvent;
import cn.smu.edu.interaction.domain.dto.*;
import cn.smu.edu.interaction.domain.entity.*;
import cn.smu.edu.interaction.domain.vo.RollCallVO;
import cn.smu.edu.interaction.repository.*;
import cn.smu.edu.interaction.service.AttendanceQueryService;
import cn.smu.edu.interaction.service.InteractionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionServiceImpl implements InteractionService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final BarrageMapper barrageMapper;
    private final RandomCallMapper randomCallMapper;
    private final SlideFeedbackMapper slideFeedbackMapper;
    private final ClassScoreMapper classScoreMapper;
    private final AttendanceMapper attendanceMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void sendBarrage(Long lessonId, Long studentId, BarrageDTO dto) {
        Barrage barrage = new Barrage();
        barrage.setLessonId(lessonId);
        barrage.setStudentId(studentId);
        barrage.setContent(dto.getContent());
        barrage.setStyle(dto.getStyle());
        barrage.setIsBlocked(0);
        barrageMapper.insert(barrage);

        // Kafka 广播通知（edu-notify 消费后推 STOMP /topic/lesson/{id}/barrage）
        TeachingEvent event = new TeachingEvent("BARRAGE", lessonId, studentId, Map.of(
                "content", dto.getContent(),
                "style", dto.getStyle() == null ? "roll" : dto.getStyle()
        ));
        kafkaTemplate.send(KafkaTopic.TEACHING_EVENTS, lessonId.toString(), event);

        log.info("弹幕发送: lessonId={}, studentId={}", lessonId, studentId);
    }

    @Override
    public void blockBarrage(Long barrageId, Long teacherId) {
        barrageMapper.update(null, new LambdaUpdateWrapper<Barrage>()
                .eq(Barrage::getId, barrageId)
                .set(Barrage::getIsBlocked, 1));
        log.info("弹幕已屏蔽: barrageId={}, teacherId={}", barrageId, teacherId);
    }

    @Override
    public RollCallVO rollCall(Long lessonId, Long teacherId, RollCallDTO dto) {
        // 获取候选学生池
        List<Long> candidates;
        if (dto.isExcludeAbsent()) {
            // 只从已签到学生中抽（C3-07 验收要求）
            candidates = attendanceMapper.selectAttendedStudentIds(lessonId);
        } else {
            // 从所有学生中抽（含未签到）
            candidates = attendanceMapper.selectList(
                    new LambdaQueryWrapper<Attendance>()
                            .eq(Attendance::getLessonId, lessonId)
                            .select(Attendance::getStudentId)
            ).stream().map(Attendance::getStudentId).collect(Collectors.toList());
        }

        if (candidates.isEmpty()) {
            return RollCallVO.builder()
                    .lessonId(lessonId)
                    .studentIds(List.of())
                    .style(dto.getStyle())
                    .message("没有可点名的学生")
                    .build();
        }

        // 随机抽取（Fisher-Yates shuffle 前 N 个）
        int pickCount = Math.min(dto.getCount(), candidates.size());
        Collections.shuffle(candidates, RANDOM);
        List<Long> picked = candidates.subList(0, pickCount);

        // 记录入库
        RandomCall record = new RandomCall();
        record.setLessonId(lessonId);
        record.setTeacherId(teacherId);
        record.setStyle(dto.getStyle());
        try {
            record.setStudentIds(objectMapper.writeValueAsString(picked));
        } catch (Exception e) {
            record.setStudentIds("[]");
        }
        randomCallMapper.insert(record);

        // Kafka 广播（edu-notify 消费后推 STOMP /topic/lesson/{id}/roll-call）
        TeachingEvent event = new TeachingEvent("ROLL_CALL", lessonId, teacherId, Map.of(
                "studentIds", picked,
                "style", dto.getStyle() == null ? "random" : dto.getStyle()
        ));
        kafkaTemplate.send(KafkaTopic.TEACHING_EVENTS, lessonId.toString(), event);

        log.info("随机点名: lessonId={}, picked={}, style={}", lessonId, picked, dto.getStyle());

        return RollCallVO.builder()
                .lessonId(lessonId)
                .studentIds(new ArrayList<>(picked))
                .style(dto.getStyle())
                .message("点名成功，共 " + picked.size() + " 人")
                .build();
    }

    @Override
    public void slideFeedback(Long lessonId, Long studentId, SlideFeedbackDTO dto) {
        SlideFeedback fb = new SlideFeedback();
        fb.setLessonId(lessonId);
        fb.setStudentId(studentId);
        fb.setSlidePage(dto.getSlidePage());
        fb.setKeyword(dto.getKeyword());
        fb.setFeedbackType(dto.getFeedbackType());
        slideFeedbackMapper.insert(fb);
    }

    @Override
    public List<Map<String, Object>> slideFeedbackStats(Long lessonId) {
        return slideFeedbackMapper.countByPage(lessonId);
    }

    @Override
    public void addScore(Long lessonId, Long classId, Long operatorId, ClassScoreDTO dto) {
        ClassScore score = new ClassScore();
        score.setLessonId(lessonId);
        score.setStudentId(dto.getStudentId());
        score.setClassId(classId);
        score.setScore(dto.getScore());
        score.setReason(dto.getReason());
        score.setOperatorId(operatorId);
        classScoreMapper.insert(score);

        log.info("课堂积分: lessonId={}, studentId={}, score={}", lessonId, dto.getStudentId(), dto.getScore());
    }
}
