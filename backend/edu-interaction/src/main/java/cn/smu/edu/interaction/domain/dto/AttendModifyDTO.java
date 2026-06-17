package cn.smu.edu.interaction.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AttendModifyDTO {

    /** 0-缺勤 1-正常签到 2-迟到 3-请假 */
    @Min(0) @Max(3)
    private Integer status;

    private String remark;
}
