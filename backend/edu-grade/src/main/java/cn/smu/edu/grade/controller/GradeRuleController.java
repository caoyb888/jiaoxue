package cn.smu.edu.grade.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.grade.domain.dto.GradeRuleCreateDTO;
import cn.smu.edu.grade.domain.dto.GradeRuleUpdateDTO;
import cn.smu.edu.grade.domain.vo.GradeRuleListVO;
import cn.smu.edu.grade.domain.vo.GradeRuleVO;
import cn.smu.edu.grade.service.GradeRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/grade/rules")
@RequiredArgsConstructor
public class GradeRuleController {

    private final GradeRuleService gradeRuleService;

    /** 新增成绩规则（权重合计不得超过 100） */
    @OperationLog(module = "grade", operation = "创建成绩规则")
    @PostMapping
    public Result<GradeRuleVO> create(@Valid @RequestBody GradeRuleCreateDTO dto) {
        return Result.ok(gradeRuleService.create(UserContext.getUserId(), dto));
    }

    /** 修改成绩规则 */
    @OperationLog(module = "grade", operation = "修改成绩规则")
    @PutMapping("/{ruleId}")
    public Result<GradeRuleVO> update(
            @PathVariable Long ruleId,
            @Valid @RequestBody GradeRuleUpdateDTO dto) {
        return Result.ok(gradeRuleService.update(ruleId, UserContext.getUserId(), dto));
    }

    /** 删除成绩规则（逻辑删除） */
    @OperationLog(module = "grade", operation = "删除成绩规则")
    @DeleteMapping("/{ruleId}")
    public Result<Void> delete(@PathVariable Long ruleId) {
        gradeRuleService.delete(ruleId, UserContext.getUserId());
        return Result.ok();
    }

    /** 查询班级成绩规则列表（含权重合计及完成状态） */
    @GetMapping("/class/{classId}")
    public Result<GradeRuleListVO> listByClass(@PathVariable Long classId) {
        return Result.ok(gradeRuleService.listByClass(classId));
    }
}
