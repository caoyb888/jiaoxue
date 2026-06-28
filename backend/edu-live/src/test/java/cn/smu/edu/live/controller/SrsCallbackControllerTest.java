package cn.smu.edu.live.controller;

import cn.smu.edu.live.service.LiveStreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SrsCallbackControllerTest {

    @Mock
    LiveStreamService liveStreamService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SrsCallbackController(liveStreamService)).build();
    }

    @Test
    void onPublish_shouldReturnZeroAndDelegate() throws Exception {
        mockMvc.perform(post("/api/v1/live/srs/on_publish")
                        .contentType("application/json")
                        .content("{\"action\":\"on_publish\",\"app\":\"live\",\"stream\":\"lesson-2-x\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(is("0"))); // SRS 约定返回 0 放行

        verify(liveStreamService).onPublish("lesson-2-x");
    }

    @Test
    void onDvr_shouldReturnZeroAndDelegateWithFile() throws Exception {
        mockMvc.perform(post("/api/v1/live/srs/on_dvr")
                        .contentType("application/json")
                        .content("{\"action\":\"on_dvr\",\"stream\":\"lesson-2-x\",\"file\":\"/srs/dvr/live/lesson-2-x.flv\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(is("0")));

        verify(liveStreamService).onDvr("lesson-2-x", "/srs/dvr/live/lesson-2-x.flv");
    }
}
