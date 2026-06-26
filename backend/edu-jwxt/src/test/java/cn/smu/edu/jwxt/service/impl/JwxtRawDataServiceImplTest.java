package cn.smu.edu.jwxt.service.impl;

import cn.smu.edu.jwxt.domain.entity.JwxtRawData;
import cn.smu.edu.jwxt.repository.JwxtRawDataMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwxtRawDataServiceImplTest {

    @Mock
    JwxtRawDataMapper rawDataMapper;

    @InjectMocks
    JwxtRawDataServiceImpl service;

    @Test
    void stage_shouldShortCircuitOnEmpty() {
        assertThat(service.stage(List.of())).isZero();
        verify(rawDataMapper, never()).batchInsert(anyList());
    }

    @Test
    void stage_shouldDelegateBatchInsert() {
        List<JwxtRawData> rows = List.of(
                JwxtRawData.builder().syncLogId(1L).dataType("STUDENT").jwxtId("S1")
                        .rawJson("{}").status(0).build());
        when(rawDataMapper.batchInsert(rows)).thenReturn(1);
        assertThat(service.stage(rows)).isEqualTo(1);
    }

    @Test
    void fetchPending_shouldDelegate() {
        JwxtRawData row = JwxtRawData.builder().id(5L).dataType("STUDENT").status(0).build();
        when(rawDataMapper.selectPendingByType("STUDENT", 500)).thenReturn(List.of(row));
        assertThat(service.fetchPending("STUDENT", 500)).containsExactly(row);
    }

    @Test
    void markSuccess_shouldUseStatus1AndNullError() {
        when(rawDataMapper.updateStatusByIds(anyList(), eq(1), isNull())).thenReturn(2);
        assertThat(service.markSuccess(List.of(1L, 2L))).isEqualTo(2);
        verify(rawDataMapper).updateStatusByIds(List.of(1L, 2L), 1, null);
    }

    @Test
    void markFailed_shouldUseStatus2AndErrorMsg() {
        when(rawDataMapper.updateStatusByIds(anyList(), eq(2), eq("boom"))).thenReturn(1);
        assertThat(service.markFailed(List.of(9L), "boom")).isEqualTo(1);
        verify(rawDataMapper).updateStatusByIds(List.of(9L), 2, "boom");
    }

    @Test
    void markSuccess_shouldShortCircuitOnEmpty() {
        assertThat(service.markSuccess(List.of())).isZero();
        verify(rawDataMapper, never()).updateStatusByIds(anyList(), eq(1), isNull());
    }
}
