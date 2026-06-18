package cn.smu.edu.course.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LessonScheduleVO {

    private Long id;
    private Long classId;
    private LocalDateTime scheduledAt;
    private String repeatType;
    private LocalDateTime repeatEndAt;
    private Integer weekDay;
    private Long lessonId;
    private Integer status;
    private LocalDateTime createdAt;
}
