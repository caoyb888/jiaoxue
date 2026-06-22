package cn.smu.edu.ai.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 一键 AI 出题请求。
 */
@Data
public class QuestionGenerateDTO {

    @NotNull(message = "题库ID不能为空")
    private Long bankId;

    @NotBlank(message = "知识点/主题不能为空")
    private String topic;

    /** 题型：1-单选 2-多选 3-判断 4-填空 5-主观 */
    @NotEmpty(message = "至少选择一种题型")
    private List<Integer> types;

    @Min(value = 1, message = "题目数至少1道")
    @Max(value = 20, message = "单次最多生成20道")
    private int count;

    /** 难度 1~5，默认 3 */
    @Min(value = 1, message = "难度范围1~5")
    @Max(value = 5, message = "难度范围1~5")
    private int difficulty = 3;
}
