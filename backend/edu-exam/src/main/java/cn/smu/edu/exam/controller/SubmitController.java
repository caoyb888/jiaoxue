package cn.smu.edu.exam.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.exam.domain.dto.SubmitAnswerDTO;
import cn.smu.edu.exam.domain.vo.ExamScoreSummaryVO;
import cn.smu.edu.exam.domain.vo.SubmitResultVO;
import cn.smu.edu.exam.service.SubmitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exam/publishes")
@RequiredArgsConstructor
public class SubmitController {

    private final SubmitService submitService;

    /**
     * 学生交卷（简化版，C2 打散机制在 S5 实现）。
     * 客观题立即自动批改，主观题/填空题 reviewStatus=0 待人工。
     */
    @OperationLog(module = "exam", operation = "学生交卷")
    @PostMapping("/{publishId}/submit")
    public Result<SubmitResultVO> submit(
            @PathVariable Long publishId,
            @Valid @RequestBody SubmitAnswerDTO dto) {
        return Result.ok(submitService.submit(publishId, UserContext.getUserId(), dto));
    }

    /** 学生查询自己的成绩汇总（含各题批改结果） */
    @GetMapping("/{publishId}/my-score")
    public Result<ExamScoreSummaryVO> myScore(@PathVariable Long publishId) {
        return Result.ok(submitService.getScoreSummary(publishId, UserContext.getUserId()));
    }

    /** 教师查询指定学生成绩 */
    @GetMapping("/{publishId}/students/{studentId}/score")
    public Result<ExamScoreSummaryVO> studentScore(
            @PathVariable Long publishId,
            @PathVariable Long studentId) {
        return Result.ok(submitService.getScoreSummary(publishId, studentId));
    }
}
