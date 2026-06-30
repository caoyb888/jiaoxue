package cn.smu.edu.grade.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.grade.domain.entity.StudentGrade;
import cn.smu.edu.grade.domain.vo.OfflineImportResultVO;
import cn.smu.edu.grade.domain.vo.StudentGradeVO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeServiceImplTest {

    @Mock
    cn.smu.edu.grade.repository.StudentGradeMapper studentGradeMapper;
    @InjectMocks
    GradeServiceImpl service;

    @Test
    void listClassGrades_shouldMapEntitiesToVO() {
        StudentGrade g = StudentGrade.builder()
                .id(1L).classId(7L).studentId(2021001L)
                .attendScore(new BigDecimal("10")).examScore(new BigDecimal("60"))
                .totalScore(new BigDecimal("90")).offlineScore(new BigDecimal("85"))
                .calcStatus(1).build();
        when(studentGradeMapper.selectByClassId(7L)).thenReturn(List.of(g));

        List<StudentGradeVO> vos = service.listClassGrades(7L);

        assertThat(vos).hasSize(1);
        assertThat(vos.get(0).getStudentId()).isEqualTo(2021001L);
        assertThat(vos.get(0).getTotalScore()).isEqualByComparingTo("90");
        assertThat(vos.get(0).getOfflineScore()).isEqualByComparingTo("85");
    }

    @Test
    void importOffline_shouldUpdateExistingAndInsertMissing() throws Exception {
        StudentGrade existing = StudentGrade.builder()
                .id(11L).classId(7L).studentId(2021001L).calcStatus(1).build();
        when(studentGradeMapper.selectByClassId(7L)).thenReturn(List.of(existing));

        byte[] xlsx = buildXlsx(new Object[][]{
                {"学号", "线下成绩"},
                {"2021001", "88.5"},  // 已存在 → update
                {"2021002", "70"}     // 不存在 → insert
        });

        OfflineImportResultVO result = service.importOffline(7L,
                new MockMultipartFile("file", "g.xlsx", null, xlsx));

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailCount()).isZero();

        ArgumentCaptor<StudentGrade> upd = ArgumentCaptor.forClass(StudentGrade.class);
        verify(studentGradeMapper).updateById(upd.capture());
        assertThat(upd.getValue().getId()).isEqualTo(11L);
        assertThat(upd.getValue().getOfflineScore()).isEqualByComparingTo("88.5");

        ArgumentCaptor<StudentGrade> ins = ArgumentCaptor.forClass(StudentGrade.class);
        verify(studentGradeMapper).insert(ins.capture());
        assertThat(ins.getValue().getStudentId()).isEqualTo(2021002L);
        assertThat(ins.getValue().getOfflineScore()).isEqualByComparingTo("70");
        assertThat(ins.getValue().getCalcStatus()).isZero();
    }

    @Test
    void importOffline_shouldCollectErrorsForInvalidRows() throws Exception {
        when(studentGradeMapper.selectByClassId(7L)).thenReturn(List.of());

        byte[] xlsx = buildXlsx(new Object[][]{
                {"学号", "线下成绩"},
                {"", "80"},          // 学号空
                {"2021003", "abc"},  // 成绩非法
                {"2021004", "150"},  // 超范围
                {"2021005", "66"}    // 正常
        });

        OfflineImportResultVO result = service.importOffline(7L,
                new MockMultipartFile("file", "g.xlsx", null, xlsx));

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isEqualTo(3);
        assertThat(result.getErrors()).hasSize(3);
        verify(studentGradeMapper).insert(org.mockito.ArgumentMatchers.any(StudentGrade.class));
    }

    @Test
    void importOffline_shouldThrowBizException_whenNotAValidWorkbook() {
        when(studentGradeMapper.selectByClassId(7L)).thenReturn(List.of());
        MockMultipartFile bad = new MockMultipartFile("file", "g.xlsx", null, "not-a-xlsx".getBytes());

        assertThatThrownBy(() -> service.importOffline(7L, bad))
                .isInstanceOf(BizException.class);
        verify(studentGradeMapper, never()).insert(org.mockito.ArgumentMatchers.any(StudentGrade.class));
    }

    private byte[] buildXlsx(Object[][] rows) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("线下成绩");
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(String.valueOf(rows[r][c]));
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }
}
