package cn.smu.edu.exam.service;

import cn.smu.edu.exam.domain.vo.GradeResultVO;
import cn.smu.edu.exam.domain.entity.StudentAnswer;

/**
 * 客观题自动批改服务。
 * 主观题（type=4/5）由教师或 AI（S6）批改，此处不处理。
 */
public interface AutoGradeService {

    /**
     * 对一条作答记录进行自动批改，更新 answer.score / isCorrect / reviewStatus，
     * 返回批改结果 VO（含题型，便于调用方展示）。
     *
     * @param answer       已持久化的作答记录（id 必须有值）
     * @param questionType 题目类型（1-6）
     * @param correctAnswer 题目的标准答案（question.answer 字段）
     * @param questionScore 该题在本试卷中的分值
     */
    GradeResultVO grade(StudentAnswer answer, int questionType,
                        String correctAnswer, java.math.BigDecimal questionScore);
}
