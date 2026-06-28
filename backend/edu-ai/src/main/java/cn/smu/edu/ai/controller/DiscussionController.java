package cn.smu.edu.ai.controller;

import cn.smu.edu.ai.domain.document.AiGroupDiscussion;
import cn.smu.edu.ai.domain.vo.GroupDiscussionVO;
import cn.smu.edu.ai.service.DiscussionService;
import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分组讨论 AI 汇总 API（S8-04），供教师端查看与手动触发汇总。
 */
@RestController
@RequestMapping("/api/v1/ai/discussion")
@RequiredArgsConstructor
public class DiscussionController {

    private final DiscussionService discussionService;

    /** 查看某组讨论汇总（未产生则 data 为 null）。 */
    @GetMapping("/{lessonId}/group/{groupId}")
    public Result<GroupDiscussionVO> get(@PathVariable Long lessonId, @PathVariable Long groupId) {
        AiGroupDiscussion doc = discussionService.getDiscussion(lessonId, groupId);
        return Result.ok(doc == null ? null : GroupDiscussionVO.of(doc));
    }

    /** 手动触发某组讨论的 LLM 汇总。 */
    @PostMapping("/{lessonId}/group/{groupId}/summarize")
    @OperationLog(module = "ai", operation = "触发分组讨论AI汇总")
    public Result<GroupDiscussionVO> summarize(@PathVariable Long lessonId, @PathVariable Long groupId) {
        AiGroupDiscussion doc = discussionService.summarize(lessonId, groupId);
        return Result.ok(doc == null ? null : GroupDiscussionVO.of(doc));
    }
}
