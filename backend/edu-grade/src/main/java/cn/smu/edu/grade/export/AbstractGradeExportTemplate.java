package cn.smu.edu.grade.export;

import cn.smu.edu.grade.domain.entity.StudentGrade;

import java.math.BigDecimal;

/**
 * 模板公共算法：平时成绩 = 考勤 + 小测 + 互动；空值按 0 处理。
 */
public abstract class AbstractGradeExportTemplate implements GradeExportTemplate {

    /** 平时成绩 = 考勤 + 课堂小测 + 互动积分。 */
    protected BigDecimal regularScore(StudentGrade g) {
        return nz(g.getAttendScore()).add(nz(g.getQuizScore())).add(nz(g.getInteractionScore()));
    }

    protected static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
