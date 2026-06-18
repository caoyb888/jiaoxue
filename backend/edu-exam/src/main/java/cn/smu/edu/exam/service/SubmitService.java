package cn.smu.edu.exam.service;

import cn.smu.edu.exam.domain.dto.SubmitAnswerDTO;
import cn.smu.edu.exam.domain.dto.AnswerItemDTO;
import cn.smu.edu.exam.domain.vo.ExamScoreSummaryVO;
import cn.smu.edu.exam.domain.vo.SubmitResultVO;

import java.util.List;

public interface SubmitService {

    /**
     * C2 三层容灾交卷：
     * 1. Redis SETNX 幂等键（TTL 30min）→ 重复提交立即返回
     * 2. 答案 JSON 存 Redis（TTL 2h）
     * 3. 发 Kafka edu.exam.submit → 立即返回"处理中"
     */
    SubmitResultVO submit(Long publishId, Long studentId, SubmitAnswerDTO dto);

    /**
     * Kafka Consumer 调用：将 exam_submit_event 写入 exam_submit_queue 表（幂等）。
     * concurrency=10（CLAUDE.md §5.6约定）。
     */
    void enqueueSubmit(Long publishId, Long studentId, String answersJson,
                       String submitType, java.time.LocalDateTime clientSubmitAt);

    /**
     * XXL-Job 调用：从 exam_submit_queue 取 process_status=0 的记录，
     * 展开到 student_answer 并自动批改客观题。
     */
    void expandSubmitQueue(int batchSize);

    /** 查询学生本次考试得分汇总（含各题批改明细） */
    ExamScoreSummaryVO getScoreSummary(Long publishId, Long studentId);
}
