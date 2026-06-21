package cn.smu.edu.exam.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** 交卷请求（C2三层容灾：Redis幂等→Redis暂存→Kafka→exam_submit_queue→student_answer）。 */
@Data
public class SubmitAnswerDTO {

    @NotEmpty(message = "答案列表不能为空")
    @Valid
    private List<AnswerItemDTO> answers;

    /** 提交类型：MANUAL（主动交卷）/ AUTO（倒计时自动交卷）/ FORCE（教师强制收卷） */
    private String submitType = "MANUAL";

    /** 客户端打散后的实际触发时间（由前端 useAutoSubmit hook 传入，null 时服务器时间补） */
    private java.time.LocalDateTime clientSubmitAt;
}
