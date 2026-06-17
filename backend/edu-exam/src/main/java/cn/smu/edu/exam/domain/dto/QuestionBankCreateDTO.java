package cn.smu.edu.exam.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuestionBankCreateDTO {

    @NotBlank(message = "题库名称不能为空")
    @Size(max = 100, message = "题库名称不超过100字符")
    private String bankName;

    @Size(max = 500, message = "描述不超过500字符")
    private String description;

    /** 0-私有 1-院系共享，默认私有 */
    private Integer isPublic = 0;
}
