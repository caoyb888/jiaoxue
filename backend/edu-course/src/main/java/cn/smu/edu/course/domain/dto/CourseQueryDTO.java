package cn.smu.edu.course.domain.dto;

import lombok.Data;

@Data
public class CourseQueryDTO {

    private String semester;
    private Long deptId;
    private String keyword;
    private int page = 1;
    private int size = 20;
}
