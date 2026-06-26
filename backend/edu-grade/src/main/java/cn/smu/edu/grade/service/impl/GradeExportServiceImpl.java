package cn.smu.edu.grade.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.grade.domain.entity.StudentGrade;
import cn.smu.edu.grade.export.GradeExportTemplate;
import cn.smu.edu.grade.export.GradeExportTemplateProvider;
import cn.smu.edu.grade.repository.StudentGradeMapper;
import cn.smu.edu.grade.service.GradeExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * {@link GradeExportService} 实现：查 {@code student_grade}，按厂商模板用 POI 生成 xlsx。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeExportServiceImpl implements GradeExportService {

    private final StudentGradeMapper studentGradeMapper;
    private final GradeExportTemplateProvider templateProvider;

    @Override
    public byte[] exportClassGrades(Long classId, String format) {
        GradeExportTemplate template = templateProvider.resolve(format);
        List<StudentGrade> grades = studentGradeMapper.selectByClassId(classId);
        List<String> headers = template.headers();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(template.sheetName());
            writeHeader(workbook, sheet, headers);

            int rowIdx = 1;
            for (StudentGrade g : grades) {
                writeRow(sheet.createRow(rowIdx++), template.row(g));
            }
            for (int c = 0; c < headers.size(); c++) {
                sheet.autoSizeColumn(c);
            }

            workbook.write(out);
            log.info("成绩回传导出完成: classId={}, format={}, 行数={}", classId, format, grades.size());
            return out.toByteArray();
        } catch (IOException e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR.getCode(), "成绩导出失败: " + e.getMessage());
        }
    }

    private void writeHeader(Workbook workbook, Sheet sheet, List<String> headers) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font bold = workbook.createFont();
        bold.setBold(true);
        headerStyle.setFont(bold);

        Row header = sheet.createRow(0);
        for (int c = 0; c < headers.size(); c++) {
            Cell cell = header.createCell(c);
            cell.setCellValue(headers.get(c));
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeRow(Row row, List<Object> values) {
        for (int c = 0; c < values.size(); c++) {
            Cell cell = row.createCell(c);
            Object v = values.get(c);
            if (v == null) {
                cell.setBlank();
            } else if (v instanceof Number n) { // Long 与 BigDecimal 均为 Number
                cell.setCellValue(n.doubleValue());
            } else {
                cell.setCellValue(String.valueOf(v));
            }
        }
    }
}
