package cn.smu.edu.stat.controller;

import cn.smu.edu.stat.domain.vo.LessonRealtimeVO;
import cn.smu.edu.stat.domain.vo.RealtimeOverviewVO;
import cn.smu.edu.stat.service.RealtimeStatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RealtimeStatControllerTest {

    @Mock
    RealtimeStatService realtimeStatService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // standalone：只装载本 Controller，避开网关 JWT 过滤与全局 AOP
        mockMvc = MockMvcBuilders.standaloneSetup(new RealtimeStatController(realtimeStatService)).build();
    }

    @Test
    void overview_shouldReturnResultWrappedVO() throws Exception {
        when(realtimeStatService.overview())
                .thenReturn(new RealtimeOverviewVO(5, 3L, 42L, Map.of("BARRAGE", 15L)));

        mockMvc.perform(get("/api/v1/stat/realtime/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.windowMinutes").value(5))
                .andExpect(jsonPath("$.data.activeLessonCount").value(3))
                .andExpect(jsonPath("$.data.onlineStudentCount").value(42))
                .andExpect(jsonPath("$.data.eventVolume.BARRAGE").value(15));
    }

    @Test
    void lessonRealtime_shouldReturnResultWrappedVO() throws Exception {
        when(realtimeStatService.lessonRealtime(77L))
                .thenReturn(new LessonRealtimeVO(77L, 5, 8L, Map.of("ATTEND", 8L)));

        mockMvc.perform(get("/api/v1/stat/realtime/lesson/77"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.lessonId").value(77))
                .andExpect(jsonPath("$.data.onlineStudentCount").value(8))
                .andExpect(jsonPath("$.data.eventVolume.ATTEND").value(8));
    }
}
