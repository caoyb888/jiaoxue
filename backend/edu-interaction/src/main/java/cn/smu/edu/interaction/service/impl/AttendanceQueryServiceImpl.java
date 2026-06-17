package cn.smu.edu.interaction.service.impl;

import cn.smu.edu.interaction.domain.dto.AttendModifyDTO;
import cn.smu.edu.interaction.domain.entity.Attendance;
import cn.smu.edu.interaction.domain.vo.AttendanceItemVO;
import cn.smu.edu.interaction.domain.vo.AttendanceListVO;
import cn.smu.edu.interaction.repository.AttendanceMapper;
import cn.smu.edu.interaction.service.AttendanceQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceQueryServiceImpl implements AttendanceQueryService {

    private static final Map<Integer, String> STATUS_LABEL = Map.of(
            0, "缺勤",
            1, "已签到",
            2, "迟到",
            3, "请假"
    );

    private final AttendanceMapper attendanceMapper;

    @Override
    public AttendanceListVO listByLesson(Long lessonId) {
        List<Attendance> records = attendanceMapper.selectList(
                new LambdaQueryWrapper<Attendance>()
                        .eq(Attendance::getLessonId, lessonId)
                        .orderByAsc(Attendance::getAttendedAt)
        );

        List<AttendanceItemVO> items = records.stream().map(att -> {
            AttendanceItemVO item = new AttendanceItemVO();
            item.setStudentId(att.getStudentId());
            item.setStudentName("学生" + att.getStudentId()); // 可通过 Feign 调用 edu-user 服务获取真实姓名
            item.setStatus(att.getStatus());
            item.setStatusLabel(STATUS_LABEL.getOrDefault(att.getStatus(), "未知"));
            item.setMethod(att.getMethod());
            item.setAttendedAt(att.getAttendedAt());
            item.setIsModified(att.getIsModified());
            return item;
        }).collect(Collectors.toList());

        int attendedCount = (int) records.stream().filter(a -> a.getStatus() != 0).count();
        int totalStudents = records.size();

        AttendanceListVO vo = new AttendanceListVO();
        vo.setLessonId(lessonId);
        vo.setTotalStudents(totalStudents);
        vo.setAttendedCount(attendedCount);
        vo.setAbsentCount(totalStudents - attendedCount);
        vo.setAttendRate(totalStudents > 0 ? (double) attendedCount / totalStudents * 100 : 0.0);
        vo.setItems(items);
        return vo;
    }

    @Override
    public void modifyAttendance(Long lessonId, Long studentId, Long teacherId, AttendModifyDTO dto) {
        Attendance existing = attendanceMapper.selectOne(
                new LambdaQueryWrapper<Attendance>()
                        .eq(Attendance::getLessonId, lessonId)
                        .eq(Attendance::getStudentId, studentId)
        );

        if (existing == null) {
            // 教师手动补签（原本没有记录的情况）
            Attendance manual = new Attendance();
            manual.setLessonId(lessonId);
            manual.setStudentId(studentId);
            manual.setStatus(dto.getStatus());
            manual.setMethod("MANUAL");
            manual.setAttendedAt(LocalDateTime.now());
            manual.setIsModified(1);
            manual.setModifierId(teacherId);
            attendanceMapper.insert(manual);
        } else {
            attendanceMapper.update(null, new LambdaUpdateWrapper<Attendance>()
                    .eq(Attendance::getLessonId, lessonId)
                    .eq(Attendance::getStudentId, studentId)
                    .set(Attendance::getStatus, dto.getStatus())
                    .set(Attendance::getIsModified, 1)
                    .set(Attendance::getModifierId, teacherId)
            );
        }

        log.info("教师修改考勤: lessonId={}, studentId={}, status={}, teacherId={}", lessonId, studentId, dto.getStatus(), teacherId);
    }
}
