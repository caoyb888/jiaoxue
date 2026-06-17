package cn.smu.edu.exam.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.exam.domain.dto.QuestionCreateDTO;
import cn.smu.edu.exam.domain.dto.QuestionQueryDTO;
import cn.smu.edu.exam.domain.dto.QuestionUpdateDTO;
import cn.smu.edu.exam.domain.vo.QuestionVO;
import cn.smu.edu.exam.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exam/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @OperationLog(module = "exam", operation = "创建题目")
    @PostMapping
    public Result<QuestionVO> create(@Valid @RequestBody QuestionCreateDTO dto) {
        Long userId = UserContext.getUserId();
        Long deptId = UserContext.getDeptId();
        return Result.ok(questionService.create(dto, userId, userId, deptId));
    }

    @OperationLog(module = "exam", operation = "更新题目")
    @PutMapping("/{questionId}")
    public Result<QuestionVO> update(
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionUpdateDTO dto) {
        Long teacherId = UserContext.getUserId();
        Long deptId = UserContext.getDeptId();
        return Result.ok(questionService.update(questionId, dto, teacherId, deptId));
    }

    @OperationLog(module = "exam", operation = "删除题目")
    @DeleteMapping("/{questionId}")
    public Result<Void> delete(@PathVariable Long questionId) {
        Long teacherId = UserContext.getUserId();
        Long deptId = UserContext.getDeptId();
        questionService.delete(questionId, teacherId, deptId);
        return Result.ok();
    }

    @GetMapping("/{questionId}")
    public Result<QuestionVO> getById(@PathVariable Long questionId) {
        Long teacherId = UserContext.getUserId();
        Long deptId = UserContext.getDeptId();
        return Result.ok(questionService.getById(questionId, teacherId, deptId));
    }

    @GetMapping
    public Result<PageResult<QuestionVO>> list(QuestionQueryDTO query) {
        Long teacherId = UserContext.getUserId();
        Long deptId = UserContext.getDeptId();
        return Result.ok(questionService.list(query, teacherId, deptId));
    }
}
