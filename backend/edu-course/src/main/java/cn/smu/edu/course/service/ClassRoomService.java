package cn.smu.edu.course.service;

import cn.smu.edu.course.domain.dto.ClassRoomCreateDTO;
import cn.smu.edu.course.domain.dto.StudentBatchDTO;
import cn.smu.edu.course.domain.vo.ClassRoomDetailVO;
import cn.smu.edu.course.domain.vo.ClassRoomVO;

import java.util.List;

public interface ClassRoomService {

    List<ClassRoomVO> listMyClasses(Long userId, Integer userType, String semester, Integer status);

    Long createClassRoom(Long teacherId, Long deptId, ClassRoomCreateDTO dto);

    ClassRoomDetailVO getClassDetail(Long classId);

    void addStudents(Long classId, StudentBatchDTO dto);

    void removeStudent(Long classId, Long studentId);
}
