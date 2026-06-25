package cn.smu.edu.stat.controller;

import cn.smu.edu.stat.domain.vo.ClassDailyStatVO;
import cn.smu.edu.stat.domain.vo.ClassHistoryVO;
import cn.smu.edu.stat.service.HistoryStatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HistoryStatControllerTest {

    @Mock
    HistoryStatService historyStatService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // standalone：只装载本 Controller，避开网关 JWT 过滤与全局 AOP
        mockMvc = MockMvcBuilders.standaloneSetup(new HistoryStatController(historyStatService)).build();
    }

    @Test
    void classHistory_shouldReturnResultWrappedVO_withDefaultDays() throws Exception {
        ClassDailyStatVO day = new ClassDailyStatVO("2026-06-25", 2, 50, 30, 5, 3, 12, 48);
        when(historyStatService.classHistory(7L, 30))
                .thenReturn(new ClassHistoryVO(7L, "2026-05-27", "2026-06-25", List.of(day)));

        mockMvc.perform(get("/api/v1/stat/history/class/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.classId").value(7))
                .andExpect(jsonPath("$.data.fromDate").value("2026-05-27"))
                .andExpect(jsonPath("$.data.daily[0].statDate").value("2026-06-25"))
                .andExpect(jsonPath("$.data.daily[0].attendCount").value(50))
                .andExpect(jsonPath("$.data.daily[0].activeStudentCount").value(48));

        verify(historyStatService).classHistory(7L, 30);
    }

    @Test
    void classHistory_shouldClampDaysAboveMaxTo180() throws Exception {
        when(historyStatService.classHistory(eq(7L), eq(180)))
                .thenReturn(new ClassHistoryVO(7L, "2025-12-28", "2026-06-25", List.of()));

        mockMvc.perform(get("/api/v1/stat/history/class/7").param("days", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 越界天数被规整到上限 180
        verify(historyStatService).classHistory(7L, 180);
    }

    @Test
    void classHistory_shouldClampDaysBelowOneTo1() throws Exception {
        when(historyStatService.classHistory(eq(7L), eq(1)))
                .thenReturn(new ClassHistoryVO(7L, "2026-06-25", "2026-06-25", List.of()));

        mockMvc.perform(get("/api/v1/stat/history/class/7").param("days", "0"))
                .andExpect(status().isOk());

        verify(historyStatService).classHistory(7L, 1);
    }
}
