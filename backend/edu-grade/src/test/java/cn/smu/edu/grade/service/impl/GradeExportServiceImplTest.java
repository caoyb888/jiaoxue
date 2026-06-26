package cn.smu.edu.grade.service.impl;

import cn.smu.edu.grade.domain.entity.StudentGrade;
import cn.smu.edu.grade.export.GradeExportTemplateProvider;
import cn.smu.edu.grade.export.QiangzhiGradeTemplate;
import cn.smu.edu.grade.export.ZhengfangGradeTemplate;
import cn.smu.edu.grade.repository.StudentGradeMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeExportServiceImplTest {

    @Mock
    StudentGradeMapper studentGradeMapper;

    private GradeExportServiceImpl service() {
        GradeExportTemplateProvider provider = new GradeExportTemplateProvider(
                List.of(new ZhengfangGradeTemplate(), new QiangzhiGradeTemplate()));
        return new GradeExportServiceImpl(studentGradeMapper, provider);
    }

    @Test
    void exportClassGrades_shouldProduceXlsxWithHeaderAndRows_zhengfang() throws Exception {
        StudentGrade g = StudentGrade.builder()
                .studentId(2021001L)
                .attendScore(new BigDecimal("10")).quizScore(new BigDecimal("15"))
                .interactionScore(new BigDecimal("5")).examScore(new BigDecimal("60"))
                .totalScore(new BigDecimal("90")).build();
        when(studentGradeMapper.selectByClassId(7L)).thenReturn(List.of(g));

        byte[] bytes = service().exportClassGrades(7L, "zhengfang");

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("成绩回传");
            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("学号");
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo("总评成绩");

            Row data = sheet.getRow(1);
            assertThat(data.getCell(0).getNumericCellValue()).isEqualTo(2021001d);
            assertThat(data.getCell(1).getNumericCellValue()).isEqualTo(30d); // 平时 10+15+5
            assertThat(data.getCell(3).getNumericCellValue()).isEqualTo(90d); // 总评
        }
    }

    @Test
    void exportClassGrades_shouldHandleEmptyClassWithHeaderOnly_qiangzhi() throws Exception {
        when(studentGradeMapper.selectByClassId(8L)).thenReturn(List.of());

        byte[] bytes = service().exportClassGrades(8L, "qiangzhi");

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("成绩导入");
            assertThat(sheet.getRow(0).getCell(4).getStringCellValue()).isEqualTo("备注");
            assertThat(sheet.getLastRowNum()).isZero(); // 仅表头行
        }
    }
}
