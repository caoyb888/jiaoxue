package cn.smu.edu.exam.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.exam.domain.dto.AnswerItemDTO;
import cn.smu.edu.exam.domain.dto.SubmitAnswerDTO;
import cn.smu.edu.exam.domain.entity.*;
import cn.smu.edu.exam.domain.vo.*;
import cn.smu.edu.exam.repository.*;
import cn.smu.edu.exam.service.AutoGradeService;
import cn.smu.edu.exam.service.SubmitService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmitServiceImpl implements SubmitService {

    private final ExamPublishMapper publishMapper;
    private final ExamPaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final StudentAnswerMapper studentAnswerMapper;
    private final AutoGradeService autoGradeService;

    @Override
    @Transactional
    public SubmitResultVO submit(Long publishId, Long studentId, SubmitAnswerDTO dto) {
        // 1. 验证考试发布存在
        ExamPublish publish = publishMapper.selectById(publishId);
        if (publish == null) {
            throw new BizException(ErrorCode.EXAM_NOT_FOUND);
        }
        // 2. 验证考试状态（简化版：不校验 status，完整 C2 约束在 S5）
        // S5 会加：status 必须为 1（进行中），且交卷打散机制

        // 3. 构建题目得分 Map（questionId → score in paper）
        List<ExamPaperQuestion> paperQuestions =
                paperQuestionMapper.selectByPaperId(publish.getPaperId());
        Map<Long, BigDecimal> scoreMap = paperQuestions.stream()
                .collect(Collectors.toMap(ExamPaperQuestion::getQuestionId,
                        ExamPaperQuestion::getScore,
                        (a, b) -> a)); // 同一题只取第一个（A卷）

        // 4. 批量加载题目（获取类型和标准答案）
        Set<Long> questionIds = dto.getAnswers().stream()
                .map(AnswerItemDTO::getQuestionId)
                .collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionIds.isEmpty()
                ? Collections.emptyMap()
                : questionMapper.selectBatchIds(questionIds).stream()
                        .collect(Collectors.toMap(Question::getId, q -> q));

        // 5. 保存作答记录 + 自动批改
        List<GradeResultVO> gradeResults = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (AnswerItemDTO item : dto.getAnswers()) {
            Question question = questionMap.get(item.getQuestionId());
            if (question == null) {
                log.warn("交卷中包含未知题目: questionId={}, publishId={}", item.getQuestionId(), publishId);
                continue;
            }

            // 幂等：已存在的作答不重复插入（uk_publish_question_student）
            if (studentAnswerMapper.existsAnswer(publishId, item.getQuestionId(), studentId) > 0) {
                log.warn("作答已存在，跳过: publishId={}, questionId={}, studentId={}",
                        publishId, item.getQuestionId(), studentId);
                continue;
            }

            StudentAnswer answer = new StudentAnswer();
            answer.setPublishId(publishId);
            answer.setQuestionId(item.getQuestionId());
            answer.setStudentId(studentId);
            answer.setAnswerContent(item.getAnswerContent());
            answer.setReviewStatus(0); // 初始未批改
            answer.setSubmittedAt(now);
            studentAnswerMapper.insert(answer);

            // 自动批改（客观题 + 投票题）
            BigDecimal questionScore = scoreMap.getOrDefault(item.getQuestionId(), question.getScore());
            GradeResultVO result = autoGradeService.grade(
                    answer, question.getType(), question.getAnswer(), questionScore);
            gradeResults.add(result);
        }

        log.info("交卷完成: publishId={}, studentId={}, answers={}, graded={}",
                publishId, studentId, dto.getAnswers().size(), gradeResults.size());

        SubmitResultVO vo = new SubmitResultVO();
        vo.setPublishId(publishId);
        vo.setStudentId(studentId);
        vo.setSubmittedCount(gradeResults.size());
        vo.setGradeResults(gradeResults);
        return vo;
    }

    @Override
    public ExamScoreSummaryVO getScoreSummary(Long publishId, Long studentId) {
        ExamPublish publish = publishMapper.selectById(publishId);
        if (publish == null) {
            throw new BizException(ErrorCode.EXAM_NOT_FOUND);
        }

        // 试卷满分
        Long paperId = publish.getPaperId();
        BigDecimal fullScore = paperQuestionMapper.sumScoreByPaperId(paperId);

        List<StudentAnswer> answers =
                studentAnswerMapper.selectByPublishAndStudent(publishId, studentId);

        BigDecimal totalScore = studentAnswerMapper.sumScoreByPublishAndStudent(publishId, studentId);
        int graded   = studentAnswerMapper.countGraded(publishId, studentId);
        int correct  = studentAnswerMapper.countCorrect(publishId, studentId);

        List<StudentAnswerVO> answerVOs = answers.stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        ExamScoreSummaryVO summary = new ExamScoreSummaryVO();
        summary.setPublishId(publishId);
        summary.setStudentId(studentId);
        summary.setTotalScore(totalScore);
        summary.setFullScore(fullScore);
        summary.setTotalQuestions(answers.size());
        summary.setGradedQuestions(graded);
        summary.setCorrectCount(correct);
        summary.setAnswers(answerVOs);
        return summary;
    }

    private StudentAnswerVO toVO(StudentAnswer a) {
        StudentAnswerVO vo = new StudentAnswerVO();
        vo.setId(a.getId());
        vo.setPublishId(a.getPublishId());
        vo.setQuestionId(a.getQuestionId());
        vo.setStudentId(a.getStudentId());
        vo.setAnswerContent(a.getAnswerContent());
        vo.setScore(a.getScore());
        vo.setIsCorrect(a.getIsCorrect());
        vo.setComment(a.getComment());
        vo.setReviewStatus(a.getReviewStatus());
        vo.setSubmittedAt(a.getSubmittedAt());
        return vo;
    }
}
