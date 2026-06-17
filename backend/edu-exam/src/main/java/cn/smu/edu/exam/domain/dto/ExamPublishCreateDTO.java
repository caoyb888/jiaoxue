package cn.smu.edu.exam.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamPublishCreateDTO {

    @NotNull(message = "试卷ID不能为空")
    private Long paperId;

    @NotNull(message = "班级ID不能为空")
    private Long classId;

    @NotNull(message = "考试开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "考试截止时间不能为空")
    private LocalDateTime endTime;

    @NotNull(message = "考试时长不能为空")
    @Min(value = 1, message = "考试时长至少1分钟")
    @Max(value = 600, message = "考试时长不超过600分钟")
    private Integer durationMin = 60;

    /** 明文密码，服务层 BCrypt 加密存储；null=不设密码 */
    private String password;

    /** 0-不开启 1-开启在线监考 */
    private Integer enableMonitor = 0;

    /** 0-不核验 1-证件照 2-现场拍照 */
    @Min(0) @Max(2)
    private Integer faceVerifyType = 0;

    /** null=发布后立即可查答案 */
    private LocalDateTime answerShowAt;

    /** 0-禁止复制 1-允许 */
    private Integer allowCopy = 0;

    /** 0-固定顺序 1-乱序题目 */
    private Integer shuffleQuestion = 0;

    /** 0-固定顺序 1-乱序选项 */
    private Integer shuffleOption = 0;
}
