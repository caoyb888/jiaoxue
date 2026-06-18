package cn.smu.edu.course.domain.vo;

import lombok.Data;

@Data
public class LessonEndVO {

    private Long lessonId;
    private Integer status;
    private Integer durationMin;
    private Boolean aiTaskTriggered;
    private String message;
}
