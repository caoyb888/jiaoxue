package cn.smu.edu.live.controller;

import cn.smu.edu.live.domain.vo.LiveConfigVO;
import cn.smu.edu.live.service.LiveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LiveControllerTest {

    @Mock
    LiveService liveService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LiveController(liveService)).build();
    }

    @Test
    void start_slideOnly_shouldReturnC5DisabledConfig() throws Exception {
        when(liveService.startLive(any(), any())).thenReturn(LiveConfigVO.slideOnly(1L));

        mockMvc.perform(post("/api/v1/live/1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.liveMode").value("SLIDE_ONLY"))
                .andExpect(jsonPath("$.data.webrtcEnabled").value(false))
                .andExpect(jsonPath("$.data.rtmpEnabled").value(false))
                .andExpect(jsonPath("$.data.pushUrl").doesNotExist());
    }

    @Test
    void start_onlineClass_shouldReturnStreamUrls() throws Exception {
        when(liveService.startLive(any(), any())).thenReturn(new LiveConfigVO(
                2L, "ONLINE_CLASS", true, true, "lesson-2-x",
                "rtmp://srs/live/lesson-2-x", "http://srs/live/lesson-2-x.m3u8", 0));

        mockMvc.perform(post("/api/v1/live/2/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.webrtcEnabled").value(true))
                .andExpect(jsonPath("$.data.pushUrl").value("rtmp://srs/live/lesson-2-x"));
    }
}
