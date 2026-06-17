package cn.smu.edu.exam.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerItemDTO {

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    /** 作答内容：客观题传选项标签（如"A"或"A,C"），判断题传"T"/"F"，主观题传富文本内容 */
    private String answerContent;
}
