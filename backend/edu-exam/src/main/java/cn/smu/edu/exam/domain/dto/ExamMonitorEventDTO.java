package cn.smu.edu.exam.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 监考异常事件上报 DTO。
 * eventType: TAB_SWITCH | SCREENSHOT | COPY
 */
@Data
public class ExamMonitorEventDTO {

    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    private String detail;
}
