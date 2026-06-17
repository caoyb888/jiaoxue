package cn.smu.edu.interaction.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class AttendanceListVO {

    private Long lessonId;

    private int totalStudents;

    private int attendedCount;

    private int absentCount;

    private double attendRate;

    private List<AttendanceItemVO> items;
}
