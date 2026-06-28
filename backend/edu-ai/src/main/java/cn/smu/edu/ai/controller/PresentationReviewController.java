package cn.smu.edu.ai.controller;

import cn.smu.edu.ai.domain.document.AiPresentationReview;
import cn.smu.edu.ai.domain.dto.PresentationReviewDTO;
import cn.smu.edu.ai.domain.vo.PresentationReviewVO;
import cn.smu.edu.ai.service.PresentationReviewService;
import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 汇报点评 API（S8-05）。教师对汇报转写发起 AI 多维评分并查看结果。
 */
@RestController
@RequestMapping("/api/v1/ai/presentation")
@RequiredArgsConstructor
public class PresentationReviewController {

    private final PresentationReviewService presentationReviewService;

    /** 对一份汇报转写做 AI 多维评分。 */
    @PostMapping("/review")
    @OperationLog(module = "ai", operation = "汇报AI点评")
    public Result<PresentationReviewVO> review(@Valid @RequestBody PresentationReviewDTO dto) {
        return Result.ok(PresentationReviewVO.of(presentationReviewService.review(dto)));
    }

    /** 查看某学生汇报点评（未产生则 data 为 null）。 */
    @GetMapping("/{lessonId}/student/{studentId}")
    public Result<PresentationReviewVO> get(@PathVariable Long lessonId, @PathVariable Long studentId) {
        AiPresentationReview r = presentationReviewService.getReview(lessonId, studentId);
        return Result.ok(r == null ? null : PresentationReviewVO.of(r));
    }
}
