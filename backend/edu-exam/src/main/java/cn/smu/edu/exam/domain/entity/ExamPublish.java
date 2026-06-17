package cn.smu.edu.exam.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 试卷发布配置表。
 * status: 0-预约未开始 1-进行中 2-已结束 3-已取消
 * face_verify_type: 0-不核验 1-证件照 2-现场拍照
 * password_hash: BCrypt 散列（NULL=不设密码）
 */
@Data
@TableName("exam_publish")
public class ExamPublish {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long paperId;
    private Long classId;
    private Long teacherId;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMin;

    /** BCrypt 散列，NULL=不设密码。注意：永远不要序列化到响应体 */
    private String passwordHash;

    private Integer enableMonitor;
    private Integer faceVerifyType;
    private LocalDateTime answerShowAt;
    private Integer allowCopy;
    private Integer shuffleQuestion;
    private Integer shuffleOption;

    /** 0-预约未开始 1-进行中 2-已结束 3-已取消（DB冗余字段，由定时任务维护） */
    private Integer status;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
