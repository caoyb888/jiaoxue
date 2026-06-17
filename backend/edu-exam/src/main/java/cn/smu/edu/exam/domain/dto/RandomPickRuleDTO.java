package cn.smu.edu.exam.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/** 随机抽题规则：从指定题库按条件随机抽取 count 道题 */
@Data
public class RandomPickRuleDTO {

    @NotNull(message = "题库ID不能为空")
    private Long bankId;

    /** 1-单选 2-多选 3-判断 4-填空 5-主观 6-投票；null=不限类型 */
    @Min(1) @Max(6)
    private Integer type;

    /** 1-5难度；null=不限难度 */
    @Min(1) @Max(5)
    private Integer difficulty;

    @NotNull(message = "抽取数量不能为空")
    @Min(value = 1, message = "至少抽取1道题")
    private Integer count;

    @NotNull(message = "每题分值不能为空")
    @DecimalMin(value = "0.01", message = "每题分值必须大于0")
    private BigDecimal scorePerQuestion;

    /** 大题名称，如"一、单选题" */
    private String section;

    /** A/B/C 卷组，默认A */
    @Pattern(regexp = "[ABC]", message = "卷组只能是 A/B/C")
    private String paperGroup = "A";
}
