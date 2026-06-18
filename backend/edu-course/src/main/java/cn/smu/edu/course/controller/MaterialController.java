package cn.smu.edu.course.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.course.domain.dto.MaterialCompleteDTO;
import cn.smu.edu.course.domain.dto.MaterialUploadDTO;
import cn.smu.edu.course.domain.vo.MaterialCompleteVO;
import cn.smu.edu.course.domain.vo.MaterialListItemVO;
import cn.smu.edu.course.domain.vo.MaterialUploadVO;
import cn.smu.edu.course.service.MaterialService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/course/material")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @OperationLog(module = "course", operation = "申请课件上传凭证")
    @PostMapping("/upload")
    public Result<MaterialUploadVO> applyUpload(@RequestBody @Valid MaterialUploadDTO dto) {
        return Result.ok(materialService.applyUpload(UserContext.getUserId(), dto));
    }

    @OperationLog(module = "course", operation = "完成课件上传")
    @PostMapping("/upload/complete")
    public Result<MaterialCompleteVO> completeUpload(@RequestBody @Valid MaterialCompleteDTO dto) {
        return Result.ok(materialService.completeUpload(UserContext.getUserId(), dto));
    }

    @GetMapping("/list")
    public Result<PageResult<MaterialListItemVO>> listMaterials(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return Result.ok(materialService.listMaterials(UserContext.getUserId(), keyword, page, size));
    }
}
