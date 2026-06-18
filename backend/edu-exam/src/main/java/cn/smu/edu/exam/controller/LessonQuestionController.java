package cn.smu.edu.exam.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.exam.domain.dto.PublishLessonQuestionDTO;
import cn.smu.edu.exam.domain.vo.LessonQuestionVO;
import cn.smu.edu.exam.service.LessonQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exam/lessons")
@RequiredArgsConstructor
public class LessonQuestionController {

    private final LessonQuestionService lessonQuestionService;

    /** 教师向课堂发布题目（同时关闭上一道进行中的题目） */
    @OperationLog(module = "exam", operation = "课堂发题")
    @PostMapping("/{lessonId}/questions")
    public Result<LessonQuestionVO> publish(
            @PathVariable Long lessonId,
            @Valid @RequestBody PublishLessonQuestionDTO dto) {
        return Result.ok(lessonQuestionService.publish(
                lessonId, UserContext.getUserId(), dto));
    }

    /** 教师手动关闭当前题目作答 */
    @OperationLog(module = "exam", operation = "关闭课堂题目")
    @PutMapping("/{lessonId}/questions/{lessonQuestionId}/close")
    public Result<Void> close(
            @PathVariable Long lessonId,
            @PathVariable Long lessonQuestionId) {
        lessonQuestionService.close(lessonQuestionId, UserContext.getUserId());
        return Result.ok();
    }

    /** 获取当前进行中的题目（学生/教师均可轮询） */
    @GetMapping("/{lessonId}/questions/current")
    public Result<LessonQuestionVO> getCurrent(@PathVariable Long lessonId) {
        return Result.ok(lessonQuestionService.getCurrent(lessonId));
    }

    /** 获取本节课发题历史 */
    @GetMapping("/{lessonId}/questions/history")
    public Result<List<LessonQuestionVO>> getHistory(@PathVariable Long lessonId) {
        return Result.ok(lessonQuestionService.getHistory(lessonId));
    }
}
