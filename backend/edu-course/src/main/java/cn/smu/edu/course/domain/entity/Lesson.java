package cn.smu.edu.course.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lesson")
public class Lesson {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long classId;
    private Long teacherId;
    private Long materialId;
    private String title;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String liveMode;
    private String livePushUrl;
    private String livePlayUrl;
    private String replayUrl;
    private Integer replayVisible;
    private String chapter;
    private Integer currentSlide;
    private Integer isScheduled;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
