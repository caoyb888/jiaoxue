package cn.smu.edu.exam.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 教师阅卷 DTO。
 * review_status 由后端自动更新为 2（教师已批改），不需前端传入。
 */
@Data
public class ReviewAnswerDTO {

    @NotNull(message = "分值不能为空")
    @DecimalMin(value = "0", message = "分值不能为负")
    private BigDecimal score;

    /** 是否正确（主观题：0错误/1正确/留null由教师给分决定） */
    private Integer isCorrect;

    private String comment;
}
