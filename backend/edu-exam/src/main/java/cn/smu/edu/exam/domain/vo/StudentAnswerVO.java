package cn.smu.edu.exam.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StudentAnswerVO {
    private Long id;
    private Long publishId;
    private Long questionId;
    private Long studentId;
    private String answerContent;
    private BigDecimal score;
    /** NULL=未批改 0=错误 1=正确 */
    private Integer isCorrect;
    private String comment;
    /** 0-未批改 1-自动批改完成 2-教师已批改 */
    private Integer reviewStatus;
    private LocalDateTime submittedAt;
    /** 主观题附件路径列表（MinIO key），阅卷时展示 */
    private List<String> attachments;
}
