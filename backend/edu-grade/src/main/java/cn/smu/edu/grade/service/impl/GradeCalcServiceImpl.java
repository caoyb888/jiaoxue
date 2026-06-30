package cn.smu.edu.grade.service.impl;

import cn.smu.edu.grade.domain.entity.GradeRule;
import cn.smu.edu.grade.domain.entity.StudentGrade;
import cn.smu.edu.grade.repository.GradeRuleMapper;
import cn.smu.edu.grade.repository.StudentGradeMapper;
import cn.smu.edu.grade.service.GradeCalcService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * {@link GradeCalcService} 实现。
 *
 * <p>权重来源 grade_rule（按 grade_type 映射到 student_grade 的四个在线维度，归一化后加权）；
 * 无规则则用默认权重。综合总分 = 考勤·w + 小测·w + 互动·w + 考试·w（权重乘法）。
 * 线下成绩（offline_score）为教师导入的独立项，不参与自动加权（见 S8-09）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeCalcServiceImpl implements GradeCalcService {

    private static final int STATUS_CALCULATED = 1;

    /** grade_type → 维度。1期末/3实验→考试；2平时→小测；4项目/6其他→互动；5出勤→考勤。 */
    private static String dimensionOf(Integer gradeType) {
        if (gradeType == null) {
            return "exam";
        }
        return switch (gradeType) {
            case 5 -> "attend";
            case 2 -> "quiz";
            case 4, 6 -> "interaction";
            default -> "exam"; // 1 期末 / 3 实验 / 其他
        };
    }

    /** 无 grade_rule 时的默认权重（考勤 0.1 / 小测 0.2 / 互动 0.2 / 考试 0.5）。 */
    static final Weights DEFAULT_WEIGHTS = new Weights(
            new BigDecimal("0.10"), new BigDecimal("0.20"),
            new BigDecimal("0.20"), new BigDecimal("0.50"));

    private final GradeRuleMapper gradeRuleMapper;
    private final StudentGradeMapper studentGradeMapper;

    @Override
    public int calculateClass(Long classId) {
        Weights w = resolveWeights(gradeRuleMapper.selectByClassId(classId));
        List<StudentGrade> grades = studentGradeMapper.selectByClassId(classId);
        int count = 0;
        for (StudentGrade g : grades) {
            BigDecimal total = nz(g.getAttendScore()).multiply(w.attend())
                    .add(nz(g.getQuizScore()).multiply(w.quiz()))
                    .add(nz(g.getInteractionScore()).multiply(w.interaction()))
                    .add(nz(g.getExamScore()).multiply(w.exam()))
                    .setScale(2, RoundingMode.HALF_UP);
            StudentGrade update = new StudentGrade();
            update.setId(g.getId());
            update.setTotalScore(total);
            update.setCalcStatus(STATUS_CALCULATED);
            studentGradeMapper.updateById(update);
            count++;
        }
        log.info("综合成绩计算: classId={}, 学生数={}, 权重={}", classId, count, w);
        return count;
    }

    @Override
    public int calculatePending() {
        List<Long> classIds = studentGradeMapper.selectPendingClassIds();
        int total = 0;
        for (Long classId : classIds) {
            total += calculateClass(classId);
        }
        log.info("待计算成绩处理完成: 班级数={}, 学生数={}", classIds.size(), total);
        return total;
    }

    /** 按 grade_type 聚合权重并归一化为分数（和为 1）；无有效规则则用默认权重。 */
    Weights resolveWeights(List<GradeRule> rules) {
        BigDecimal attend = BigDecimal.ZERO, quiz = BigDecimal.ZERO,
                interaction = BigDecimal.ZERO, exam = BigDecimal.ZERO, total = BigDecimal.ZERO;
        for (GradeRule r : rules) {
            BigDecimal weight = nz(r.getWeight());
            total = total.add(weight);
            switch (dimensionOf(r.getGradeType())) {
                case "attend" -> attend = attend.add(weight);
                case "quiz" -> quiz = quiz.add(weight);
                case "interaction" -> interaction = interaction.add(weight);
                default -> exam = exam.add(weight);
            }
        }
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return DEFAULT_WEIGHTS;
        }
        return new Weights(
                frac(attend, total), frac(quiz, total), frac(interaction, total), frac(exam, total));
    }

    private static BigDecimal frac(BigDecimal part, BigDecimal total) {
        return part.divide(total, 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 四个在线维度的归一化权重（和为 1）。 */
    record Weights(BigDecimal attend, BigDecimal quiz, BigDecimal interaction, BigDecimal exam) {
    }
}
