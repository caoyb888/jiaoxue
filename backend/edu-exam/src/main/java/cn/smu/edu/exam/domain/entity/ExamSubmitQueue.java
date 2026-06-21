package cn.smu.edu.exam.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交卷流水暂存表（C2核心约束：高并发交卷容灾）。
 * process_status: 0-待处理 1-处理中 2-已展开到明细表 3-处理失败
 * submit_type: MANUAL/AUTO/FORCE
 */
@Data
@TableName("exam_submit_queue")
public class ExamSubmitQueue {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long publishId;
    private Long studentId;
    private String answersJson;
    private String submitType;
    private LocalDateTime clientSubmitAt;
    private Integer processStatus;
    private Integer retryCount;
    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;
}
