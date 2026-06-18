package cn.smu.edu.course.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LessonDetailVO {

    private Long id;
    private Long classId;
    private String className;
    private Long teacherId;
    private String teacherName;
    private String title;
    private String chapter;
    private Integer status;
    private String liveMode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMin;
    private MaterialVO material;
    private Integer currentSlide;
    private String replayUrl;
    private Boolean replayVisible;

    @Data
    public static class MaterialVO {
        private Long id;
        private String title;
        private Integer pageCount;
        private String slideDir;
    }
}
