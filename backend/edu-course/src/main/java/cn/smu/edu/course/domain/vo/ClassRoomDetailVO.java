package cn.smu.edu.course.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ClassRoomDetailVO {

    private Long id;
    private String courseName;
    private String className;
    private TeacherVO teacher;
    private Integer studentCount;
    private String semester;
    private Integer status;
    private List<StudentVO> students;

    @Data
    public static class TeacherVO {
        private Long id;
        private String realName;
        private String avatarUrl;
    }

    @Data
    public static class StudentVO {
        private Long id;
        private String realName;
        private String studentNo;
        private String groupName;
    }
}
