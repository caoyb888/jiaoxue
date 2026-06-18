package cn.smu.edu.course.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.course.domain.dto.CourseCreateDTO;
import cn.smu.edu.course.domain.dto.CourseQueryDTO;
import cn.smu.edu.course.domain.vo.CourseListItemVO;
import cn.smu.edu.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping("/list")
    public Result<PageResult<CourseListItemVO>> listCourses(CourseQueryDTO query) {
        return Result.ok(courseService.listCourses(query));
    }

    @OperationLog(module = "course", operation = "创建课程")
    @PostMapping
    public Result<Map<String, Long>> createCourse(@RequestBody @Valid CourseCreateDTO dto) {
        Long id = courseService.createCourse(dto);
        return Result.ok(Map.of("id", id));
    }
}
