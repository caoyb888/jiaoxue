package cn.smu.edu.course.domain.vo;

import lombok.Data;

@Data
public class ClassRoomVO {

    private Long id;
    private String courseName;
    private String courseCode;
    private String className;
    private String classCode;
    private String teacherName;
    private Integer studentCount;
    private String semester;
    private Integer status;
    private String deptName;
}
