package cn.smu.edu.course.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LessonScheduleCreateDTO {

    @NotNull
    private Long classId;

    @NotNull
    private LocalDateTime scheduledAt;

    private String repeatType = "NONE";
    private LocalDateTime repeatEndAt;
    private Integer weekDay;
}
