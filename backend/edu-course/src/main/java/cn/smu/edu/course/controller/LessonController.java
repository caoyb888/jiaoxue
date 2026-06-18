package cn.smu.edu.course.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.course.domain.dto.LessonQueryDTO;
import cn.smu.edu.course.domain.dto.LessonScheduleCreateDTO;
import cn.smu.edu.course.domain.dto.LessonStartDTO;
import cn.smu.edu.course.domain.vo.LessonDetailVO;
import cn.smu.edu.course.domain.vo.LessonEndVO;
import cn.smu.edu.course.domain.vo.LessonScheduleVO;
import cn.smu.edu.course.domain.vo.LessonStartVO;
import cn.smu.edu.course.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/course/lesson")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @OperationLog(module = "course", operation = "开始课堂")
    @PostMapping("/start")
    public Result<LessonStartVO> startLesson(@RequestBody @Valid LessonStartDTO dto) {
        return Result.ok(lessonService.startLesson(UserContext.getUserId(), dto));
    }

    @OperationLog(module = "course", operation = "结束课堂")
    @PostMapping("/{lessonId}/end")
    public Result<LessonEndVO> endLesson(@PathVariable Long lessonId) {
        return Result.ok(lessonService.endLesson(lessonId, UserContext.getUserId()));
    }

    @GetMapping("/list")
    public Result<PageResult<LessonDetailVO>> listLessons(@Valid LessonQueryDTO query) {
        return Result.ok(lessonService.listLessons(query));
    }

    @GetMapping("/{lessonId}")
    public Result<LessonDetailVO> getLessonDetail(@PathVariable Long lessonId) {
        return Result.ok(lessonService.getLessonDetail(lessonId));
    }

    @PostMapping("/{lessonId}/slide")
    public Result<Void> updateSlide(@PathVariable Long lessonId,
                                    @RequestBody Map<String, Integer> body) {
        Integer slideNo = body.get("slideNo");
        lessonService.updateCurrentSlide(lessonId, slideNo, UserContext.getUserId());
        return Result.ok();
    }

    @OperationLog(module = "course", operation = "创建排课")
    @PostMapping("/schedule")
    public Result<Map<String, Long>> createSchedule(@RequestBody @Valid LessonScheduleCreateDTO dto) {
        Long id = lessonService.createSchedule(UserContext.getUserId(), dto);
        return Result.ok(Map.of("id", id));
    }

    @GetMapping("/schedule/{scheduleId}")
    public Result<LessonScheduleVO> getSchedule(@PathVariable Long scheduleId) {
        return Result.ok(lessonService.getSchedule(scheduleId));
    }
}
