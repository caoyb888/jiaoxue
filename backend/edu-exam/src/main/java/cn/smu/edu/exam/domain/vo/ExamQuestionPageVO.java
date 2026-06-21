package cn.smu.edu.exam.domain.vo;

import lombok.Data;

import java.util.List;

/** 分批下题响应（GET /exam/{publishId}/questions?page=N）。 */
@Data
public class ExamQuestionPageVO {
    private List<PaperQuestionVO> questions;
    private Integer totalQuestions;
    private Integer totalPages;
    private Integer currentPage;
    private Integer pageSize;
}
