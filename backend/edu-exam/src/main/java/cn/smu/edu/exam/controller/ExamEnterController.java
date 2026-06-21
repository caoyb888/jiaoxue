package cn.smu.edu.exam.controller;

import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.exam.domain.dto.ExamEnterDTO;
import cn.smu.edu.exam.domain.vo.ExamEnterVO;
import cn.smu.edu.exam.domain.vo.ExamQuestionPageVO;
import cn.smu.edu.exam.service.ExamEnterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exam/publishes")
@RequiredArgsConstructor
public class ExamEnterController {

    private final ExamEnterService examEnterService;

    /**
     * 进入考试（学生端）。
     * 密码正确 → 创建/恢复监考记录 → 返回首页题目（10题/页）。
     * 若 faceVerifyType>0，sessionStatus=VERIFYING，须完成人脸核验后方可作答。
     */
    @PostMapping("/{publishId}/enter")
    public Result<ExamEnterVO> enter(
            @PathVariable Long publishId,
            @RequestBody(required = false) ExamEnterDTO dto) {
        return Result.ok(examEnterService.enter(publishId, UserContext.getUserId(), dto));
    }

    /**
     * 分批获取题目（每页10题，禁止一次性返回全卷）。
     * 须先调用 /enter 建立会话，否则返回 100508。
     */
    @GetMapping("/{publishId}/questions")
    public Result<ExamQuestionPageVO> getQuestionsPage(
            @PathVariable Long publishId,
            @RequestParam(defaultValue = "1") int page) {
        return Result.ok(examEnterService.getQuestionsPage(publishId, UserContext.getUserId(), page));
    }
}
