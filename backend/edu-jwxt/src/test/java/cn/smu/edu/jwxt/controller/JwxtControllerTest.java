package cn.smu.edu.jwxt.controller;

import cn.smu.edu.jwxt.service.JwxtFullSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class JwxtControllerTest {

    @Mock
    JwxtFullSyncService fullSyncService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new JwxtController(fullSyncService)).build();
    }

    @Test
    void fullSync_shouldReturnSyncLogIdWrappedInResult() throws Exception {
        when(fullSyncService.fullSync(any())).thenReturn(456L);

        mockMvc.perform(post("/api/v1/jwxt/sync/full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(456));

        verify(fullSyncService).fullSync(any());
    }
}
