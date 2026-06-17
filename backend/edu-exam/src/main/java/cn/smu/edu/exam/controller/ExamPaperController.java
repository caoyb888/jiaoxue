package cn.smu.edu.exam.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.exam.domain.dto.*;
import cn.smu.edu.exam.domain.vo.ExamPaperDetailVO;
import cn.smu.edu.exam.domain.vo.ExamPaperVO;
import cn.smu.edu.exam.domain.vo.ScoreCheckVO;
import cn.smu.edu.exam.service.ExamPaperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exam/papers")
@RequiredArgsConstructor
public class ExamPaperController {

    private final ExamPaperService examPaperService;

    @OperationLog(module = "exam", operation = "创建试卷")
    @PostMapping
    public Result<ExamPaperVO> create(@Valid @RequestBody ExamPaperCreateDTO dto) {
        return Result.ok(examPaperService.create(dto, UserContext.getUserId()));
    }

    @OperationLog(module = "exam", operation = "更新试卷")
    @PutMapping("/{paperId}")
    public Result<ExamPaperVO> update(
            @PathVariable Long paperId,
            @Valid @RequestBody ExamPaperUpdateDTO dto) {
        return Result.ok(examPaperService.update(paperId, dto, UserContext.getUserId()));
    }

    @OperationLog(module = "exam", operation = "删除试卷")
    @DeleteMapping("/{paperId}")
    public Result<Void> delete(@PathVariable Long paperId) {
        examPaperService.delete(paperId, UserContext.getUserId());
        return Result.ok();
    }

    @GetMapping("/{paperId}")
    public Result<ExamPaperDetailVO> getDetail(@PathVariable Long paperId) {
        return Result.ok(examPaperService.getDetail(paperId, UserContext.getUserId()));
    }

    @GetMapping
    public Result<PageResult<ExamPaperVO>> list(ExamPaperQueryDTO query) {
        return Result.ok(examPaperService.list(query, UserContext.getUserId()));
    }

    @OperationLog(module = "exam", operation = "试卷添加题目")
    @PostMapping("/{paperId}/questions/batch")
    public Result<ExamPaperDetailVO> addQuestions(
            @PathVariable Long paperId,
            @Valid @RequestBody BatchAddQuestionsDTO dto) {
        return Result.ok(examPaperService.addQuestions(paperId, dto, UserContext.getUserId()));
    }

    @OperationLog(module = "exam", operation = "试卷移除题目")
    @DeleteMapping("/{paperId}/questions/{questionId}")
    public Result<Void> removeQuestion(
            @PathVariable Long paperId,
            @PathVariable Long questionId,
            @RequestParam(defaultValue = "A") String paperGroup) {
        examPaperService.removeQuestion(paperId, questionId, paperGroup, UserContext.getUserId());
        return Result.ok();
    }

    @OperationLog(module = "exam", operation = "随机组卷")
    @PostMapping("/{paperId}/questions/random")
    public Result<ExamPaperDetailVO> randomCompose(
            @PathVariable Long paperId,
            @Valid @RequestBody RandomCompositionDTO dto) {
        return Result.ok(examPaperService.randomCompose(paperId, dto, UserContext.getUserId()));
    }

    @GetMapping("/{paperId}/score-check")
    public Result<ScoreCheckVO> checkScore(@PathVariable Long paperId) {
        return Result.ok(examPaperService.checkScore(paperId, UserContext.getUserId()));
    }
}
