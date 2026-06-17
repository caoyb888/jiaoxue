package cn.smu.edu.exam.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class QuestionOptionDTO {

    @Pattern(regexp = "[A-E]", message = "选项标签只能是 A-E")
    private String optionLabel;

    @NotBlank(message = "选项内容不能为空")
    private String content;

    /** 0-错误 1-正确 */
    private Integer isCorrect = 0;

    private Integer sortOrder;
}
