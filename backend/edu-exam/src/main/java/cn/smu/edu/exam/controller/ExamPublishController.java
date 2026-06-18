package cn.smu.edu.exam.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.exam.domain.dto.*;
import cn.smu.edu.exam.domain.vo.ExamPublishStudentVO;
import cn.smu.edu.exam.domain.vo.ExamPublishVO;
import cn.smu.edu.exam.domain.vo.StudentExamListVO;
import cn.smu.edu.exam.service.ExamPublishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exam/publishes")
@RequiredArgsConstructor
public class ExamPublishController {

    private final ExamPublishService examPublishService;

    @OperationLog(module = "exam", operation = "发布试卷")
    @PostMapping
    public Result<ExamPublishVO> publish(@Valid @RequestBody ExamPublishCreateDTO dto) {
        return Result.ok(examPublishService.publish(dto, UserContext.getUserId()));
    }

    @OperationLog(module = "exam", operation = "修改考试配置")
    @PutMapping("/{publishId}")
    public Result<ExamPublishVO> update(
            @PathVariable Long publishId,
            @Valid @RequestBody ExamPublishUpdateDTO dto) {
        return Result.ok(examPublishService.update(publishId, dto, UserContext.getUserId()));
    }

    @OperationLog(module = "exam", operation = "取消考试")
    @DeleteMapping("/{publishId}")
    public Result<Void> cancel(@PathVariable Long publishId) {
        examPublishService.cancel(publishId, UserContext.getUserId());
        return Result.ok();
    }

    @GetMapping("/{publishId}")
    public Result<ExamPublishVO> getById(@PathVariable Long publishId) {
        return Result.ok(examPublishService.getById(publishId, UserContext.getUserId()));
    }

    @GetMapping
    public Result<PageResult<ExamPublishVO>> list(ExamPublishQueryDTO query) {
        return Result.ok(examPublishService.listByTeacher(query, UserContext.getUserId()));
    }

    /**
     * 学生视角：获取考试详情（含试题）。
     * 需设密码时在请求体中携带 password 字段。
     */
    @PostMapping("/{publishId}/student-view")
    public Result<ExamPublishStudentVO> studentView(
            @PathVariable Long publishId,
            @RequestBody(required = false) VerifyPasswordDTO passwordDTO) {
        String password = passwordDTO != null ? passwordDTO.getPassword() : null;
        return Result.ok(examPublishService.getStudentView(
                publishId, UserContext.getUserId(), password));
    }

    /** 独立密码验证接口（学生在进入考试前先调用，通过后再拉题） */
    @PostMapping("/{publishId}/verify-password")
    public Result<Boolean> verifyPassword(
            @PathVariable Long publishId,
            @Valid @RequestBody VerifyPasswordDTO dto) {
        return Result.ok(examPublishService.verifyPassword(publishId, dto.getPassword()));
    }

    /**
     * 学生端：查询班级的考试列表（含是否已进入/已交卷）。
     * 仅返回摘要信息，不含题目内容。
     */
    @GetMapping("/student/list")
    public Result<List<StudentExamListVO>> listForStudent(@RequestParam Long classId) {
        return Result.ok(examPublishService.listForStudent(classId, UserContext.getUserId()));
    }
}
