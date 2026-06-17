package cn.smu.edu.exam.service;

import cn.smu.edu.exam.domain.dto.SubmitAnswerDTO;
import cn.smu.edu.exam.domain.vo.ExamScoreSummaryVO;
import cn.smu.edu.exam.domain.vo.SubmitResultVO;

public interface SubmitService {

    /** 简化版交卷：保存作答 + 自动批改客观题 */
    SubmitResultVO submit(Long publishId, Long studentId, SubmitAnswerDTO dto);

    /** 查询学生本次考试得分汇总（含各题批改明细） */
    ExamScoreSummaryVO getScoreSummary(Long publishId, Long studentId);
}
