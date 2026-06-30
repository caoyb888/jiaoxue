package cn.smu.edu.grade.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.grade.domain.vo.OfflineImportResultVO;
import cn.smu.edu.grade.domain.vo.StudentGradeVO;
import cn.smu.edu.grade.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 成绩列表查询 + 线下成绩导入 API（S8-09）。角色鉴权由网关统一处理（教师/管理员）。
 */
@RestController
@RequestMapping("/api/v1/grade")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    /** 查询教学班成绩列表（各维度得分 + 综合总分 + 线下成绩）。 */
    @GetMapping("/class/{classId}")
    public Result<List<StudentGradeVO>> listClassGrades(@PathVariable Long classId) {
        return Result.ok(gradeService.listClassGrades(classId));
    }

    /**
     * 导入线下成绩 xlsx（列：学号 | 线下成绩）到指定教学班。
     * 返回成功/失败明细，供前端展示错误行。
     */
    @OperationLog(module = "grade", operation = "导入线下成绩")
    @PostMapping("/import/offline/{classId}")
    public Result<OfflineImportResultVO> importOffline(
            @PathVariable Long classId,
            @RequestPart("file") MultipartFile file) {

        if (file.isEmpty()) {
            return Result.fail(400, "文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            return Result.fail(400, "仅支持 .xlsx 格式文件");
        }
        return Result.ok(gradeService.importOffline(classId, file));
    }
}
