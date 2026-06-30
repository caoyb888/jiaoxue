package cn.smu.edu.grade.service;

/**
 * 综合成绩计算引擎（S8-08）——按 grade_rule 权重对 student_grade 各维度加权求综合总分。
 */
public interface GradeCalcService {

    /**
     * 计算某教学班全部学生综合成绩，写回 total_score 并置 calc_status=1。
     *
     * @return 计算的学生数
     */
    int calculateClass(Long classId);

    /**
     * 计算所有待计算（calc_status=0）班级的成绩。
     *
     * @return 计算的学生总数
     */
    int calculatePending();
}
