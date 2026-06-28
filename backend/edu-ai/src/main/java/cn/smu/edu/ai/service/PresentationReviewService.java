package cn.smu.edu.ai.service;

import cn.smu.edu.ai.domain.document.AiPresentationReview;
import cn.smu.edu.ai.domain.dto.PresentationReviewDTO;

/**
 * 汇报点评服务（S8-05）——ASR 转写文本经 LLM 多维评分。
 */
public interface PresentationReviewService {

    /** 对一份汇报转写做多维 AI 评分并落库。 */
    AiPresentationReview review(PresentationReviewDTO dto);

    /** 读取某学生汇报点评（无则返回 {@code null}）。 */
    AiPresentationReview getReview(Long lessonId, Long studentId);
}
