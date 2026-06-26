package cn.smu.edu.grade.controller;

import cn.smu.edu.grade.service.GradeExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GradeExportControllerTest {

    @Mock
    GradeExportService gradeExportService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new GradeExportController(gradeExportService)).build();
    }

    @Test
    void export_shouldReturnXlsxAttachment_withDefaultZhengfang() throws Exception {
        when(gradeExportService.exportClassGrades(7L, "zhengfang")).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/v1/grade/export/7"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")));

        verify(gradeExportService).exportClassGrades(7L, "zhengfang");
    }

    @Test
    void export_shouldPassFormatParam() throws Exception {
        when(gradeExportService.exportClassGrades(9L, "qiangzhi")).thenReturn(new byte[]{9});

        mockMvc.perform(get("/api/v1/grade/export/9").param("format", "qiangzhi"))
                .andExpect(status().isOk());

        verify(gradeExportService).exportClassGrades(9L, "qiangzhi");
    }
}
