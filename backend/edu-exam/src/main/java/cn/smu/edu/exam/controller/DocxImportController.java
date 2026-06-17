package cn.smu.edu.exam.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.exam.domain.vo.DocxImportResultVO;
import cn.smu.edu.exam.service.DocxImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/exam/banks")
@RequiredArgsConstructor
public class DocxImportController {

    private final DocxImportService docxImportService;

    /**
     * 上传 docx 模板批量导入题目到指定题库。
     * 返回每道题的成功/失败明细，供前端展示错误行。
     */
    @OperationLog(module = "exam", operation = "docx批量导题")
    @PostMapping("/{bankId}/import/docx")
    public Result<DocxImportResultVO> importDocx(
            @PathVariable Long bankId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "2.00") BigDecimal defaultScore) {

        if (file.isEmpty()) {
            return Result.fail(400, "文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".docx")) {
            return Result.fail(400, "仅支持 .docx 格式文件");
        }
        return Result.ok(docxImportService.importFromDocx(
                bankId, file, UserContext.getUserId(), defaultScore));
    }
}
