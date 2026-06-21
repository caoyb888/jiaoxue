package cn.smu.edu.exam.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 主观题图片附件关联表。
 * 学生上传图片到 edu-file（MinIO），将路径写入 answer_content JSON，
 * 展开交卷时从 JSON 解析出 attachments 并写入此表，便于后续查询。
 */
@Data
@TableName("exam_answer_attachment")
public class ExamAnswerAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentAnswerId;
    private Long publishId;
    private Long studentId;
    private Long questionId;

    /** MinIO 路径，如 exam/2026/answer-{studentId}-{questionId}.jpg */
    private String fileKey;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
