package cn.smu.edu.interaction.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.interaction.domain.dto.AttendDTO;
import cn.smu.edu.interaction.domain.dto.AttendModifyDTO;
import cn.smu.edu.interaction.domain.vo.AttendCodeVO;
import cn.smu.edu.interaction.domain.vo.AttendResultVO;
import cn.smu.edu.interaction.domain.vo.AttendanceListVO;
import cn.smu.edu.interaction.service.AttendanceCodeService;
import cn.smu.edu.interaction.service.AttendanceService;
import cn.smu.edu.interaction.service.AttendanceQueryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/interaction")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceCodeService attendanceCodeService;
    private final AttendanceService attendanceService;
    private final AttendanceQueryService attendanceQueryService;

    /** 教师生成签到码（POST 每次生成新码，旧码失效） */
    @OperationLog(module = "interaction", operation = "生成签到码")
    @PostMapping("/lesson/{lessonId}/attend/code")
    public Result<AttendCodeVO> generateCode(@PathVariable Long lessonId) {
        Long teacherId = UserContext.getUserId();
        return Result.ok(attendanceCodeService.generateCode(lessonId, teacherId));
    }

    /** 教师查询当前有效签到码（刷新页面回显） */
    @GetMapping("/lesson/{lessonId}/attend/code")
    public Result<AttendCodeVO> getCurrentCode(@PathVariable Long lessonId) {
        return Result.ok(attendanceCodeService.getCurrentCode(lessonId));
    }

    /**
     * 【C1】学生签到（高并发入口）：BloomFilter 去重 → Redis 队列 → 立即返回
     * 请求方式：QR 扫码传 qrToken，口令签到传 code，二选一
     */
    @PostMapping("/lesson/{lessonId}/attend")
    public Result<AttendResultVO> attend(
            @PathVariable Long lessonId,
            @RequestBody AttendDTO dto,
            HttpServletRequest request) {
        Long studentId = UserContext.getUserId();
        dto.setIpAddress(request.getRemoteAddr());
        return Result.ok(attendanceService.attend(lessonId, studentId, dto));
    }

    /** 教师查询签到列表（含学生姓名、状态） */
    @GetMapping("/lesson/{lessonId}/attendance")
    public Result<AttendanceListVO> listAttendance(@PathVariable Long lessonId) {
        return Result.ok(attendanceQueryService.listByLesson(lessonId));
    }

    /** 教师手动修改考勤状态 */
    @OperationLog(module = "interaction", operation = "手动修改考勤")
    @PutMapping("/lesson/{lessonId}/attendance/{studentId}")
    public Result<Void> modifyAttendance(
            @PathVariable Long lessonId,
            @PathVariable Long studentId,
            @RequestBody AttendModifyDTO dto) {
        Long teacherId = UserContext.getUserId();
        attendanceQueryService.modifyAttendance(lessonId, studentId, teacherId, dto);
        return Result.ok();
    }
}
