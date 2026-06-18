package cn.smu.edu.course.domain.vo;

import lombok.Data;

@Data
public class LessonStartVO {

    private Long lessonId;
    private Integer status;
    private String liveMode;
    private String wsEndpoint;
    private String wsTopicPrefix;
}
