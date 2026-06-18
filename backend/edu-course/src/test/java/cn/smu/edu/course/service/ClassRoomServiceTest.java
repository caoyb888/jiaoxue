package cn.smu.edu.course.service;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.course.domain.dto.ClassRoomCreateDTO;
import cn.smu.edu.course.domain.dto.StudentBatchDTO;
import cn.smu.edu.course.domain.entity.ClassRoom;
import cn.smu.edu.course.domain.entity.ClassStudent;
import cn.smu.edu.course.repository.ClassRoomMapper;
import cn.smu.edu.course.repository.ClassStudentMapper;
import cn.smu.edu.course.service.impl.ClassRoomServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassRoomServiceTest {

    @Mock
    private ClassRoomMapper classRoomMapper;

    @Mock
    private ClassStudentMapper classStudentMapper;

    @InjectMocks
    private ClassRoomServiceImpl classRoomService;

    @Test
    void createClassRoom_shouldThrow_whenCodeDuplicated() {
        when(classRoomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        ClassRoomCreateDTO dto = new ClassRoomCreateDTO();
        dto.setClassCode("CS301-01");
        dto.setCourseId(1L);
        dto.setClassName("CS301测试班");
        dto.setSemester("2025-2026-1");

        assertThrows(BizException.class, () -> classRoomService.createClassRoom(1L, 1L, dto));
    }

    @Test
    void createClassRoom_shouldSucceed_whenCodeNotDuplicated() {
        when(classRoomMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(classRoomMapper.insert(any(ClassRoom.class))).thenReturn(1);

        ClassRoomCreateDTO dto = new ClassRoomCreateDTO();
        dto.setClassCode("CS301-01");
        dto.setCourseId(1L);
        dto.setClassName("CS301测试班");
        dto.setSemester("2025-2026-1");

        assertDoesNotThrow(() -> classRoomService.createClassRoom(1L, 1L, dto));
        verify(classRoomMapper).insert(any(ClassRoom.class));
    }

    @Test
    void addStudents_shouldThrow_whenStudentIdsEmpty() {
        StudentBatchDTO dto = new StudentBatchDTO();
        dto.setStudentIds(List.of());

        assertThrows(BizException.class, () -> classRoomService.addStudents(1L, dto));
    }

    @Test
    void removeStudent_shouldThrow_whenStudentNotInClass() {
        when(classStudentMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);

        assertThrows(BizException.class, () -> classRoomService.removeStudent(1L, 99L));
    }
}
