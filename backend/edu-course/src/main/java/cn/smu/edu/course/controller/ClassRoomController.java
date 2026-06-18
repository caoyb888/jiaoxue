package cn.smu.edu.course.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.course.domain.dto.ClassRoomCreateDTO;
import cn.smu.edu.course.domain.dto.StudentBatchDTO;
import cn.smu.edu.course.domain.vo.ClassRoomDetailVO;
import cn.smu.edu.course.domain.vo.ClassRoomVO;
import cn.smu.edu.course.service.ClassRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/course/class")
@RequiredArgsConstructor
public class ClassRoomController {

    private final ClassRoomService classRoomService;

    @GetMapping("/my")
    public Result<List<ClassRoomVO>> myClasses(
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) Integer status) {
        Long userId = UserContext.getUserId();
        String roles = UserContext.getRoles();
        // userType: 1=学生, 2=教师
        Integer userType = (roles != null && roles.contains("TEACHER")) ? 2 : 1;
        return Result.ok(classRoomService.listMyClasses(userId, userType, semester, status));
    }

    @OperationLog(module = "course", operation = "创建教学班")
    @PostMapping
    public Result<Map<String, Long>> createClass(@RequestBody @Valid ClassRoomCreateDTO dto) {
        Long teacherId = UserContext.getUserId();
        Long deptId = UserContext.getDeptId();
        Long id = classRoomService.createClassRoom(teacherId, deptId, dto);
        return Result.ok(Map.of("id", id));
    }

    @GetMapping("/{classId}")
    public Result<ClassRoomDetailVO> getClassDetail(@PathVariable Long classId) {
        return Result.ok(classRoomService.getClassDetail(classId));
    }

    @OperationLog(module = "course", operation = "添加班级学生")
    @PostMapping("/{classId}/students")
    public Result<Void> addStudents(@PathVariable Long classId,
                                    @RequestBody @Valid StudentBatchDTO dto) {
        classRoomService.addStudents(classId, dto);
        return Result.ok();
    }

    @OperationLog(module = "course", operation = "移除班级学生")
    @DeleteMapping("/{classId}/students/{studentId}")
    public Result<Void> removeStudent(@PathVariable Long classId,
                                      @PathVariable Long studentId) {
        classRoomService.removeStudent(classId, studentId);
        return Result.ok();
    }
}
