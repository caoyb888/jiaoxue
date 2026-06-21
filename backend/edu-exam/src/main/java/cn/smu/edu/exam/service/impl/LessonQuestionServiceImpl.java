package cn.smu.edu.exam.service.impl;

import cn.smu.edu.common.event.TeachingEvent;
import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.exam.domain.dto.PublishLessonQuestionDTO;
import cn.smu.edu.exam.domain.dto.SubmitLessonAnswerDTO;
import cn.smu.edu.exam.domain.entity.LessonQuestion;
import cn.smu.edu.exam.domain.entity.LessonQuestionAnswer;
import cn.smu.edu.exam.domain.entity.Question;
import cn.smu.edu.exam.domain.entity.QuestionOption;
import cn.smu.edu.exam.domain.vo.LessonAnswerResultVO;
import cn.smu.edu.exam.domain.vo.LessonQuestionVO;
import cn.smu.edu.exam.domain.vo.QuestionOptionVO;
import cn.smu.edu.exam.repository.LessonQuestionAnswerMapper;
import cn.smu.edu.exam.repository.LessonQuestionMapper;
import cn.smu.edu.exam.repository.QuestionMapper;
import cn.smu.edu.exam.repository.QuestionOptionMapper;
import cn.smu.edu.exam.service.LessonQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonQuestionServiceImpl implements LessonQuestionService {

    private static final int STATUS_OPEN   = 0;
    private static final int STATUS_CLOSED = 1;

    private static final java.util.Set<String> TRUE_VALUES =
            java.util.Set.of("TRUE", "T", "1", "对", "是", "正确", "√");
    private static final java.util.Set<String> FALSE_VALUES =
            java.util.Set.of("FALSE", "F", "0", "错", "否", "错误", "×");

    private final LessonQuestionMapper lessonQuestionMapper;
    private final LessonQuestionAnswerMapper lessonQuestionAnswerMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public LessonQuestionVO publish(Long lessonId, Long teacherId, PublishLessonQuestionDTO dto) {
        Question question = questionMapper.selectById(dto.getQuestionId());
        if (question == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }

        // 关闭当前进行中的题目（确保同一时刻只有一道题）
        lessonQuestionMapper.closeAllByLesson(lessonId);

        LessonQuestion lq = new LessonQuestion();
        lq.setLessonId(lessonId);
        lq.setQuestionId(dto.getQuestionId());
        lq.setTeacherId(teacherId);
        lq.setStatus(STATUS_OPEN);
        lq.setOpenedAt(LocalDateTime.now());
        lessonQuestionMapper.insert(lq);

        LessonQuestionVO vo = buildVO(lq, question);

        // 异步广播题目给所有在线学生（edu-notify 消费后推 STOMP）
        broadcastQuestion(lessonId, teacherId, vo, "QUESTION_PUBLISHED");

        log.info("课堂发题: lessonId={}, questionId={}, type={}", lessonId, question.getId(), question.getType());
        return vo;
    }

    @Override
    @Transactional
    public void close(Long lessonQuestionId, Long teacherId) {
        LessonQuestion lq = lessonQuestionMapper.selectById(lessonQuestionId);
        if (lq == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (!lq.getTeacherId().equals(teacherId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        if (lq.getStatus() == STATUS_CLOSED) {
            return; // 已关闭，幂等处理
        }
        lq.setStatus(STATUS_CLOSED);
        lq.setClosedAt(LocalDateTime.now());
        lessonQuestionMapper.updateById(lq);

        // 通知学生题目已关闭
        broadcastQuestion(lq.getLessonId(), teacherId,
                Map.of("lessonQuestionId", lessonQuestionId), "QUESTION_CLOSED");
        log.info("关闭课堂题目: lessonQuestionId={}", lessonQuestionId);
    }

    @Override
    public LessonQuestionVO getCurrent(Long lessonId) {
        LessonQuestion lq = lessonQuestionMapper.selectCurrentByLesson(lessonId);
        if (lq == null) {
            return null;
        }
        Question question = questionMapper.selectById(lq.getQuestionId());
        if (question == null) {
            return null;
        }
        return buildVO(lq, question);
    }

    @Override
    public List<LessonQuestionVO> getHistory(Long lessonId) {
        List<LessonQuestion> list = lessonQuestionMapper.selectByLesson(lessonId);
        if (list.isEmpty()) return Collections.emptyList();

        Set<Long> qIds = list.stream().map(LessonQuestion::getQuestionId).collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(qIds)
                .stream().collect(Collectors.toMap(Question::getId, q -> q));

        return list.stream()
                .filter(lq -> questionMap.containsKey(lq.getQuestionId()))
                .map(lq -> buildVO(lq, questionMap.get(lq.getQuestionId())))
                .collect(Collectors.toList());
    }

    // ── 内部方法 ─────────────────────────────────────────────────────────────

    private LessonQuestionVO buildVO(LessonQuestion lq, Question question) {
        LessonQuestionVO vo = new LessonQuestionVO();
        vo.setId(lq.getId());
        vo.setLessonId(lq.getLessonId());
        vo.setQuestionId(lq.getQuestionId());
        vo.setQuestionType(question.getType());
        vo.setContent(question.getContent());
        vo.setStatus(lq.getStatus());
        vo.setOpenedAt(lq.getOpenedAt());
        vo.setClosedAt(lq.getClosedAt());

        // 选择题/投票题加载选项（不暴露 isCorrect，防泄题）
        if (question.getType() == 1 || question.getType() == 2 || question.getType() == 6) {
            List<QuestionOption> opts = questionOptionMapper.selectByQuestionId(question.getId());
            vo.setOptions(opts.stream().map(this::toOptionVO).collect(Collectors.toList()));
        }
        return vo;
    }

    private QuestionOptionVO toOptionVO(QuestionOption o) {
        QuestionOptionVO vo = new QuestionOptionVO();
        vo.setId(o.getId());
        vo.setOptionLabel(o.getOptionLabel());
        vo.setContent(o.getContent());
        vo.setSortOrder(o.getSortOrder());
        vo.setIsCorrect(null); // 课堂发题时隐藏正确答案
        return vo;
    }

    @Override
    @Transactional
    public LessonAnswerResultVO submitAnswer(Long lessonId, Long studentId, SubmitLessonAnswerDTO dto) {
        LessonQuestion lq = lessonQuestionMapper.selectById(dto.getLessonQuestionId());
        if (lq == null || !lq.getLessonId().equals(lessonId)) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (lq.getStatus() != null && lq.getStatus() == STATUS_CLOSED) {
            throw new BizException(400, "题目已关闭，无法作答");
        }
        Question question = questionMapper.selectById(lq.getQuestionId());
        if (question == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }

        Boolean correct = gradeObjective(question.getType(), dto.getAnswer(), question.getAnswer());
        Integer isCorrectVal = correct == null ? null : (correct ? 1 : 0);

        // 幂等改答：同一学生对同一题再次提交则覆盖
        LessonQuestionAnswer existing = lessonQuestionAnswerMapper.selectByLqAndStudent(lq.getId(), studentId);
        if (existing != null) {
            existing.setAnswerContent(dto.getAnswer());
            existing.setIsCorrect(isCorrectVal);
            existing.setSubmittedAt(LocalDateTime.now());
            lessonQuestionAnswerMapper.updateById(existing);
        } else {
            LessonQuestionAnswer a = new LessonQuestionAnswer();
            a.setLessonQuestionId(lq.getId());
            a.setLessonId(lessonId);
            a.setQuestionId(question.getId());
            a.setStudentId(studentId);
            a.setAnswerContent(dto.getAnswer());
            a.setIsCorrect(isCorrectVal);
            a.setSubmittedAt(LocalDateTime.now());
            lessonQuestionAnswerMapper.insert(a);
        }

        log.info("随堂答题: lessonId={}, studentId={}, lqId={}, correct={}",
                lessonId, studentId, lq.getId(), correct);

        boolean objective = correct != null;
        return LessonAnswerResultVO.builder()
                .lessonQuestionId(lq.getId())
                .questionType(question.getType())
                .isCorrect(correct)
                .correctAnswer(objective ? question.getAnswer() : null)
                .analysis(question.getAnalysis())
                .myAnswer(dto.getAnswer())
                .build();
    }

    /** 客观题（单选/多选/判断）即时判对错；填空/主观/投票返回 null（不自动判定）。 */
    private static Boolean gradeObjective(int type, String submitted, String correctAnswer) {
        return switch (type) {
            case 1 -> normalize(submitted).equals(normalize(correctAnswer));
            case 2 -> toLabelSet(submitted).equals(toLabelSet(correctAnswer));
            case 3 -> boolNormalize(submitted).equals(boolNormalize(correctAnswer));
            default -> null; // 4-填空 5-主观 6-投票
        };
    }

    private static String normalize(String v) {
        return v == null ? "" : v.trim().toUpperCase().replace(" ", "");
    }

    private static java.util.Set<String> toLabelSet(String v) {
        java.util.Set<String> set = new java.util.TreeSet<>();
        for (String s : normalize(v).split(",")) {
            if (!s.isEmpty()) set.add(s);
        }
        return set;
    }

    private static String boolNormalize(String v) {
        String n = normalize(v);
        if (TRUE_VALUES.contains(n)) return "TRUE";
        if (FALSE_VALUES.contains(n)) return "FALSE";
        return n;
    }

    private void broadcastQuestion(Long lessonId, Long teacherId, Object payload, String eventType) {
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("question", payload);
        TeachingEvent event = new TeachingEvent(eventType, lessonId, teacherId, payloadMap);
        kafkaTemplate.send(KafkaTopic.TEACHING_EVENTS, lessonId.toString(), event);
    }
}
