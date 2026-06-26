package cn.smu.edu.stat.controller;

import cn.smu.edu.stat.domain.vo.ClassDailyStatVO;
import cn.smu.edu.stat.domain.vo.ClassHistoryVO;
import cn.smu.edu.stat.domain.vo.DeptHistoryVO;
import cn.smu.edu.stat.domain.vo.DeptPeriodStatVO;
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

    @Test
    void deptHistory_shouldReturnResultWrappedVO_withDefaultDayPeriod() throws Exception {
        DeptPeriodStatVO bucket = new DeptPeriodStatVO("2026-06-25", 4, 2, 80, 40, 6, 4, 20, 70);
        when(historyStatService.deptHistory(3L, "day", 30))
                .thenReturn(new DeptHistoryVO(3L, "day", "2026-05-27", "2026-06-25", List.of(bucket)));

        mockMvc.perform(get("/api/v1/stat/history/dept/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.deptId").value(3))
                .andExpect(jsonPath("$.data.period").value("day"))
                .andExpect(jsonPath("$.data.buckets[0].periodStart").value("2026-06-25"))
                .andExpect(jsonPath("$.data.buckets[0].classCount").value(2))
                .andExpect(jsonPath("$.data.buckets[0].activeStudentCount").value(70));

        verify(historyStatService).deptHistory(3L, "day", 30);
    }

    @Test
    void deptHistory_shouldPassThroughValidWeekPeriod() throws Exception {
        when(historyStatService.deptHistory(eq(3L), eq("week"), eq(60)))
                .thenReturn(new DeptHistoryVO(3L, "week", "2026-04-27", "2026-06-25", List.of()));

        mockMvc.perform(get("/api/v1/stat/history/dept/3").param("period", "week").param("days", "60"))
                .andExpect(status().isOk());

        verify(historyStatService).deptHistory(3L, "week", 60);
    }

    @Test
    void deptHistory_shouldFallbackIllegalPeriodToDay() throws Exception {
        when(historyStatService.deptHistory(eq(3L), eq("day"), eq(30)))
                .thenReturn(new DeptHistoryVO(3L, "day", "2026-05-27", "2026-06-25", List.of()));

        mockMvc.perform(get("/api/v1/stat/history/dept/3").param("period", "year' OR 1=1"))
                .andExpect(status().isOk());

        // 非法 period（含注入尝试）被白名单兜底为 day
        verify(historyStatService).deptHistory(3L, "day", 30);
    }
}
