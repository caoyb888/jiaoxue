package cn.smu.edu.exam.service;

import cn.smu.edu.exam.domain.dto.ReviewAnswerDTO;
import cn.smu.edu.exam.domain.vo.StudentAnswerVO;

import java.util.List;

public interface ReviewService {

    /**
     * 教师批改一条学生作答（review_status: 0/1 → 2）。
     *
     * @param answerId 学生作答 ID
     * @param teacherId 教师 ID（权限校验）
     * @param dto 批改内容（score/isCorrect/comment）
     */
    StudentAnswerVO review(Long answerId, Long teacherId, ReviewAnswerDTO dto);

    /**
     * 获取某场考试某学生的全部作答（用于阅卷页）。
     * 含主观题附件列表。
     */
    List<StudentAnswerVO> listAnswers(Long publishId, Long studentId);
}
