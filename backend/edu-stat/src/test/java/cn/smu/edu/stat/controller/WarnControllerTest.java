package cn.smu.edu.stat.controller;

import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.stat.domain.dto.WarnQueryDTO;
import cn.smu.edu.stat.domain.vo.WarnVO;
import cn.smu.edu.stat.service.WarnQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WarnControllerTest {

    @Mock
    WarnQueryService warnQueryService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new WarnController(warnQueryService)).build();
    }

    @Test
    void list_shouldReturnPagedResultWrappedVO() throws Exception {
        WarnVO vo = new WarnVO(1L, "LOW_ATTEND", "LESSON", 1001L, 1001L, 100L, 10L, 50L,
                "2026-06-26", 5, 15, "低考勤", 0, "2026-06-26T10:00:00");
        when(warnQueryService.pageWarns(any(WarnQueryDTO.class)))
                .thenReturn(PageResult.of(List.of(vo), 1, 1, 20));

        mockMvc.perform(get("/api/v1/stat/warn/list").param("warnType", "LOW_ATTEND").param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].warnType").value("LOW_ATTEND"))
                .andExpect(jsonPath("$.data.list[0].statDate").value("2026-06-26"));
    }

    @Test
    void handle_shouldDelegateToServiceAndReturnOk() throws Exception {
        mockMvc.perform(put("/api/v1/stat/warn/7/handle").param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(warnQueryService).handleWarn(eq(7L), eq(1), any());
    }
}
