package cn.smu.edu.grade.domain.vo;

import cn.smu.edu.grade.domain.entity.StudentGrade;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 教学班成绩列表行（各维度得分 + 综合总分 + 线下成绩）。 */
@Data
@Builder
public class StudentGradeVO {

    private Long id;
    private Long classId;
    private Long studentId;

    private BigDecimal attendScore;
    private BigDecimal quizScore;
    private BigDecimal interactionScore;
    private BigDecimal examScore;

    /** 综合总分（NULL=待计算）。 */
    private BigDecimal totalScore;
    /** 线下成绩（NULL=未导入）。 */
    private BigDecimal offlineScore;

    /** 计算状态：0-未计算 1-已计算。 */
    private Integer calcStatus;
    private LocalDateTime updatedAt;

    public static StudentGradeVO from(StudentGrade g) {
        return StudentGradeVO.builder()
                .id(g.getId())
                .classId(g.getClassId())
                .studentId(g.getStudentId())
                .attendScore(g.getAttendScore())
                .quizScore(g.getQuizScore())
                .interactionScore(g.getInteractionScore())
                .examScore(g.getExamScore())
                .totalScore(g.getTotalScore())
                .offlineScore(g.getOfflineScore())
                .calcStatus(g.getCalcStatus())
                .updatedAt(g.getUpdatedAt())
                .build();
    }
}
