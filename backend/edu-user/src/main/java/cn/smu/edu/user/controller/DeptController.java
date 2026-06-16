package cn.smu.edu.user.controller;

import cn.smu.edu.common.result.Result;
import cn.smu.edu.user.domain.vo.DeptVO;
import cn.smu.edu.user.service.DeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "院系管理")
@RestController
@RequestMapping("/api/v1/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @Operation(summary = "获取院系树")
    @GetMapping("/tree")
    public Result<List<DeptVO>> getDeptTree() {
        return Result.ok(deptService.getDeptTree());
    }

    @Operation(summary = "获取院系详情")
    @GetMapping("/{deptId}")
    public Result<DeptVO> getDeptById(@PathVariable Long deptId) {
        return Result.ok(deptService.getDeptById(deptId));
    }
}
