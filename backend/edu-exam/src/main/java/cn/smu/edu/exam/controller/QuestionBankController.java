package cn.smu.edu.exam.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.exam.domain.dto.QuestionBankCreateDTO;
import cn.smu.edu.exam.domain.dto.QuestionBankQueryDTO;
import cn.smu.edu.exam.domain.dto.QuestionBankUpdateDTO;
import cn.smu.edu.exam.domain.vo.QuestionBankVO;
import cn.smu.edu.exam.service.QuestionBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exam/banks")
@RequiredArgsConstructor
public class QuestionBankController {

    private final QuestionBankService questionBankService;

    @OperationLog(module = "exam", operation = "创建题库")
    @PostMapping
    public Result<QuestionBankVO> create(@Valid @RequestBody QuestionBankCreateDTO dto) {
        Long teacherId = UserContext.getUserId();
        Long deptId = UserContext.getDeptId();
        return Result.ok(questionBankService.create(dto, teacherId, deptId));
    }

    @OperationLog(module = "exam", operation = "更新题库")
    @PutMapping("/{bankId}")
    public Result<QuestionBankVO> update(
            @PathVariable Long bankId,
            @Valid @RequestBody QuestionBankUpdateDTO dto) {
        Long teacherId = UserContext.getUserId();
        return Result.ok(questionBankService.update(bankId, dto, teacherId));
    }

    @OperationLog(module = "exam", operation = "删除题库")
    @DeleteMapping("/{bankId}")
    public Result<Void> delete(@PathVariable Long bankId) {
        Long teacherId = UserContext.getUserId();
        questionBankService.delete(bankId, teacherId);
        return Result.ok();
    }

    @GetMapping("/{bankId}")
    public Result<QuestionBankVO> getById(@PathVariable Long bankId) {
        Long teacherId = UserContext.getUserId();
        Long deptId = UserContext.getDeptId();
        return Result.ok(questionBankService.getById(bankId, teacherId, deptId));
    }

    @GetMapping
    public Result<PageResult<QuestionBankVO>> list(QuestionBankQueryDTO query) {
        Long teacherId = UserContext.getUserId();
        Long deptId = UserContext.getDeptId();
        return Result.ok(questionBankService.list(query, teacherId, deptId));
    }
}
