package cn.smu.edu.course.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseListItemVO {

    private Long id;
    private String courseCode;
    private String courseName;
    private String deptName;
    private BigDecimal credit;
    private Integer courseType;
    private String semester;
    private Integer classCount;
}
