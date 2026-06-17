package cn.smu.edu.exam.service.impl;

import cn.smu.edu.exam.domain.entity.StudentAnswer;
import cn.smu.edu.exam.domain.vo.GradeResultVO;
import cn.smu.edu.exam.repository.StudentAnswerMapper;
import cn.smu.edu.exam.service.AutoGradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自动批改规则：
 *   type=1 单选：忽略大小写/空格，完全匹配得满分
 *   type=2 多选：按逗号拆分后排序再比较，完全匹配才得满分（严格多选）
 *   type=3 判断：归一化为 T/F 后比较
 *   type=4 填空：不自动批改，review_status=0
 *   type=5 主观：不自动批改，review_status=0
 *   type=6 投票：记录选择，is_correct=null，score=0，review_status=1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoGradeServiceImpl implements AutoGradeService {

    private static final int REVIEW_AUTO  = 1;
    private static final int REVIEW_NONE  = 0;

    private static final Set<String> TRUE_VALUES  = Set.of("true", "t", "1", "对", "是", "正确");
    private static final Set<String> FALSE_VALUES = Set.of("false", "f", "0", "错", "否", "错误");

    private final StudentAnswerMapper studentAnswerMapper;

    @Override
    public GradeResultVO grade(StudentAnswer answer, int questionType,
                               String correctAnswer, BigDecimal questionScore) {
        return switch (questionType) {
            case 1 -> gradeSingleChoice(answer, correctAnswer, questionScore);
            case 2 -> gradeMultipleChoice(answer, correctAnswer, questionScore);
            case 3 -> gradeTrueFalse(answer, correctAnswer, questionScore);
            case 6 -> gradeVote(answer);
            default -> skipGrade(answer, questionType); // 4=填空 5=主观，不自动批改
        };
    }

    // ── 单选题 ───────────────────────────────────────────────────────────────

    private GradeResultVO gradeSingleChoice(StudentAnswer answer,
                                             String correctAnswer,
                                             BigDecimal questionScore) {
        String submitted = normalize(answer.getAnswerContent());
        String correct   = normalize(correctAnswer);

        boolean isCorrect = submitted.equals(correct);
        BigDecimal score  = isCorrect ? questionScore : BigDecimal.ZERO;

        return persist(answer, isCorrect ? 1 : 0, score, REVIEW_AUTO, 1);
    }

    // ── 多选题 ───────────────────────────────────────────────────────────────

    private GradeResultVO gradeMultipleChoice(StudentAnswer answer,
                                               String correctAnswer,
                                               BigDecimal questionScore) {
        String submittedNorm = normalizeMultiChoice(answer.getAnswerContent());
        String correctNorm   = normalizeMultiChoice(correctAnswer);

        boolean isCorrect = submittedNorm.equals(correctNorm);
        BigDecimal score  = isCorrect ? questionScore : BigDecimal.ZERO;

        return persist(answer, isCorrect ? 1 : 0, score, REVIEW_AUTO, 2);
    }

    // ── 判断题 ───────────────────────────────────────────────────────────────

    private GradeResultVO gradeTrueFalse(StudentAnswer answer,
                                          String correctAnswer,
                                          BigDecimal questionScore) {
        String submitted = normalizeTrueFalse(answer.getAnswerContent());
        String correct   = normalizeTrueFalse(correctAnswer);

        if (submitted == null || correct == null) {
            // 无法判断，留待人工
            return skipGrade(answer, 3);
        }

        boolean isCorrect = submitted.equals(correct);
        BigDecimal score  = isCorrect ? questionScore : BigDecimal.ZERO;

        return persist(answer, isCorrect ? 1 : 0, score, REVIEW_AUTO, 3);
    }

    // ── 投票题 ───────────────────────────────────────────────────────────────

    private GradeResultVO gradeVote(StudentAnswer answer) {
        // 投票只记录选择，不判断对错，直接标记为"已处理"
        answer.setIsCorrect(null);
        answer.setScore(BigDecimal.ZERO);
        answer.setReviewStatus(REVIEW_AUTO);
        studentAnswerMapper.updateById(answer);
        return new GradeResultVO(answer.getQuestionId(), 6, BigDecimal.ZERO, null, REVIEW_AUTO);
    }

    // ── 不自动批改（填空/主观）────────────────────────────────────────────────

    private GradeResultVO skipGrade(StudentAnswer answer, int questionType) {
        // 不修改 DB，保持 review_status=0（待人工/AI批改）
        return new GradeResultVO(answer.getQuestionId(), questionType, null, null, REVIEW_NONE);
    }

    // ── 持久化并返回结果 ────────────────────────────────────────────────────

    private GradeResultVO persist(StudentAnswer answer, int isCorrect, BigDecimal score,
                                   int reviewStatus, int questionType) {
        answer.setIsCorrect(isCorrect);
        answer.setScore(score);
        answer.setReviewStatus(reviewStatus);
        studentAnswerMapper.updateById(answer);
        log.debug("自动批改: questionId={}, type={}, isCorrect={}, score={}",
                answer.getQuestionId(), questionType, isCorrect, score);
        return new GradeResultVO(answer.getQuestionId(), questionType, score, isCorrect, reviewStatus);
    }

    // ── 归一化工具方法（可独立测试）────────────────────────────────────────

    /** 去空格、转大写 */
    public static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toUpperCase();
    }

    /** 多选：拆分 → 去空格大写 → 排序 → 逗号拼接 */
    public static String normalizeMultiChoice(String s) {
        if (!StringUtils.hasText(s)) return "";
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(StringUtils::hasText)
                .sorted()
                .collect(Collectors.joining(","));
    }

    /** 判断题：归一化为 "T" 或 "F"；无法识别返回 null */
    public static String normalizeTrueFalse(String s) {
        if (!StringUtils.hasText(s)) return null;
        String lower = s.trim().toLowerCase();
        if (TRUE_VALUES.contains(lower))  return "T";
        if (FALSE_VALUES.contains(lower)) return "F";
        return null;
    }
}
