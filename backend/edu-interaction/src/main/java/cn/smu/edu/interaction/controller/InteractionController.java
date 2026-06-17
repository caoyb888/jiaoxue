package cn.smu.edu.interaction.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.interaction.domain.dto.*;
import cn.smu.edu.interaction.domain.vo.RollCallVO;
import cn.smu.edu.interaction.service.InteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/interaction")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    /** S3-06 学生发送弹幕 */
    @PostMapping("/lesson/{lessonId}/barrage")
    public Result<Void> sendBarrage(@PathVariable Long lessonId,
                                    @Valid @RequestBody BarrageDTO dto) {
        interactionService.sendBarrage(lessonId, UserContext.getUserId(), dto);
        return Result.ok();
    }

    /** S3-06 教师屏蔽弹幕 */
    @OperationLog(module = "interaction", operation = "屏蔽弹幕")
    @PutMapping("/barrage/{barrageId}/block")
    public Result<Void> blockBarrage(@PathVariable Long barrageId) {
        interactionService.blockBarrage(barrageId, UserContext.getUserId());
        return Result.ok();
    }

    /** S3-07 教师随机点名 */
    @OperationLog(module = "interaction", operation = "随机点名")
    @PostMapping("/lesson/{lessonId}/roll-call")
    public Result<RollCallVO> rollCall(@PathVariable Long lessonId,
                                       @RequestBody RollCallDTO dto) {
        return Result.ok(interactionService.rollCall(lessonId, UserContext.getUserId(), dto));
    }

    /** S3-08 学生课件反馈 */
    @PostMapping("/lesson/{lessonId}/slide-feedback")
    public Result<Void> slideFeedback(@PathVariable Long lessonId,
                                       @Valid @RequestBody SlideFeedbackDTO dto) {
        interactionService.slideFeedback(lessonId, UserContext.getUserId(), dto);
        return Result.ok();
    }

    /** S3-08 教师查看热点页面统计 */
    @GetMapping("/lesson/{lessonId}/slide-feedback/stats")
    public Result<List<Map<String, Object>>> slideFeedbackStats(@PathVariable Long lessonId) {
        return Result.ok(interactionService.slideFeedbackStats(lessonId));
    }

    /** S3-09 教师给学生加分 */
    @OperationLog(module = "interaction", operation = "课堂积分")
    @PostMapping("/lesson/{lessonId}/score")
    public Result<Void> addScore(@PathVariable Long lessonId,
                                  @RequestParam Long classId,
                                  @Valid @RequestBody ClassScoreDTO dto) {
        interactionService.addScore(lessonId, classId, UserContext.getUserId(), dto);
        return Result.ok();
    }
}
