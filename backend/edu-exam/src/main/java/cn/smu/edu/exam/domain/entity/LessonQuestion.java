package cn.smu.edu.exam.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课堂题目发布记录（与签到、弹幕并列的课堂实时互动）。
 * 一节课同时只有一道题处于 open 状态（由业务层保证）。
 * status: 0=进行中 1=已关闭
 */
@Data
@TableName("lesson_question")
public class LessonQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lessonId;
    private Long questionId;
    private Long teacherId;

    /** 0-进行中 1-已关闭 */
    private Integer status;

    private LocalDateTime openedAt;
    private LocalDateTime closedAt;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
