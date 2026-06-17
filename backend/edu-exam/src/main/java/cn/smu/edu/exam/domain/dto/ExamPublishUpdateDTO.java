package cn.smu.edu.exam.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 只允许在考试开始前修改配置 */
@Data
public class ExamPublishUpdateDTO {

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Min(value = 1, message = "考试时长至少1分钟")
    @Max(value = 600, message = "考试时长不超过600分钟")
    private Integer durationMin;

    /** null=不修改密码；空串=取消密码 */
    private String password;

    private Integer enableMonitor;

    @Min(0) @Max(2)
    private Integer faceVerifyType;

    private LocalDateTime answerShowAt;
    private Integer allowCopy;
    private Integer shuffleQuestion;
    private Integer shuffleOption;
}
