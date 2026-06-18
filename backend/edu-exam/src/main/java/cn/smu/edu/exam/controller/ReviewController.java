package cn.smu.edu.exam.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.exam.domain.dto.ReviewAnswerDTO;
import cn.smu.edu.exam.domain.vo.StudentAnswerVO;
import cn.smu.edu.exam.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exam")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 教师批改一条学生作答（主观题）。
     * review_status: 0/1 → 2（教师已批改）。
     */
    @OperationLog(module = "exam", operation = "教师阅卷批改")
    @PutMapping("/review/{answerId}")
    public Result<StudentAnswerVO> review(
            @PathVariable Long answerId,
            @Valid @RequestBody ReviewAnswerDTO dto) {
        return Result.ok(reviewService.review(answerId, UserContext.getUserId(), dto));
    }

    /**
     * 获取某学生在某场考试的全部作答列表（教师阅卷页用）。
     */
    @GetMapping("/publishes/{publishId}/students/{studentId}/answers")
    public Result<List<StudentAnswerVO>> listAnswers(
            @PathVariable Long publishId,
            @PathVariable Long studentId) {
        return Result.ok(reviewService.listAnswers(publishId, studentId));
    }
}
