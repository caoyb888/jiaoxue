package cn.smu.edu.interaction.service;

import cn.smu.edu.interaction.domain.dto.AttendModifyDTO;
import cn.smu.edu.interaction.domain.vo.AttendanceListVO;

public interface AttendanceQueryService {

    AttendanceListVO listByLesson(Long lessonId);

    void modifyAttendance(Long lessonId, Long studentId, Long teacherId, AttendModifyDTO dto);
}
