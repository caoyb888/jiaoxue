package cn.smu.edu.course.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lesson_schedule")
public class LessonSchedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long classId;
    private Long teacherId;
    private LocalDateTime scheduledAt;
    private String repeatType;
    private LocalDateTime repeatEndAt;
    private Integer weekDay;
    private Long lessonId;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
