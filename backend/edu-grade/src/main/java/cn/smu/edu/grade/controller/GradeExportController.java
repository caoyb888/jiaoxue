package cn.smu.edu.grade.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.grade.service.GradeExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 成绩回传导出 API（S7-11）。角色鉴权由网关统一处理（教师/管理员）。
 *
 * <p>导出为 xlsx 二进制下载，返回 {@link ResponseEntity}（文件流非 JSON，不套 Result）。
 */
@RestController
@RequestMapping("/api/v1/grade")
@RequiredArgsConstructor
public class GradeExportController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final GradeExportService gradeExportService;

    /**
     * 导出教学班成绩为正方/强智回传 Excel。
     *
     * @param classId 教学班 ID
     * @param format  导出格式 zhengfang（默认）/ qiangzhi
     */
    @GetMapping("/export/{classId}")
    @OperationLog(module = "grade", operation = "导出成绩回传Excel")
    public ResponseEntity<byte[]> export(@PathVariable Long classId,
                                         @RequestParam(defaultValue = "zhengfang") String format) {
        byte[] body = gradeExportService.exportClassGrades(classId, format);
        String filename = URLEncoder.encode(
                "grade_" + classId + "_" + format + ".xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(body);
    }
}
