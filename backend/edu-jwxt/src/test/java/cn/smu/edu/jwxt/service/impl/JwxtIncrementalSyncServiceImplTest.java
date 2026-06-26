package cn.smu.edu.jwxt.service.impl;

import cn.smu.edu.jwxt.adapter.JwxtDataSourceProvider;
import cn.smu.edu.jwxt.adapter.StubJwxtDataSource;
import cn.smu.edu.jwxt.domain.entity.JwxtRawData;
import cn.smu.edu.jwxt.domain.entity.JwxtSyncLog;
import cn.smu.edu.jwxt.repository.JwxtSyncLogMapper;
import cn.smu.edu.jwxt.service.JwxtMappingService;
import cn.smu.edu.jwxt.service.JwxtRawDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwxtIncrementalSyncServiceImplTest {

    @Mock
    JwxtDataSourceProvider dataSourceProvider;
    @Mock
    JwxtRawDataService rawDataService;
    @Mock
    JwxtMappingService mappingService;
    @Mock
    JwxtSyncLogMapper syncLogMapper;

    JwxtIncrementalSyncServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new JwxtIncrementalSyncServiceImpl(
                dataSourceProvider, rawDataService, mappingService, syncLogMapper);
        ReflectionTestUtils.setField(service, "batchSize", 500);
    }

    @Test
    @SuppressWarnings("unchecked")
    void incrementalSync_shouldStageAllTypesAndCloseLogWithCounts() {
        when(dataSourceProvider.active()).thenReturn(new StubJwxtDataSource());
        // 无历史成功同步 → 走默认回溯
        when(syncLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>(1, 1));
        // insert 时回填自增 id
        doAnswer(inv -> {
            ((JwxtSyncLog) inv.getArgument(0)).setId(99L);
            return 1;
        }).when(syncLogMapper).insert(any(JwxtSyncLog.class));
        when(rawDataService.stage(anyList())).thenReturn(2);
        when(mappingService.resolveLocalId(anyString(), anyString())).thenReturn(null); // 全新增

        long logId = service.incrementalSync("MANUAL");

        assertThat(logId).isEqualTo(99L);
        // 4 个数据类型各一批 → stage 调用 4 次
        verify(rawDataService, times(4)).stage(anyList());

        ArgumentCaptor<JwxtSyncLog> doneCaptor = ArgumentCaptor.forClass(JwxtSyncLog.class);
        verify(syncLogMapper).updateById(doneCaptor.capture());
        JwxtSyncLog done = doneCaptor.getValue();
        assertThat(done.getStatus()).isEqualTo(1);              // 成功
        assertThat(done.getStudentCnt()).isEqualTo(2);          // STUDENT 2
        assertThat(done.getDeptCnt()).isEqualTo(2);             // DEPT 2
        assertThat(done.getCourseCnt()).isEqualTo(4);           // COURSE 2 + CLASS 2
        assertThat(done.getSuccessCnt()).isEqualTo(8);          // 合计 8
        assertThat(done.getCostMs()).isNotNull();
        assertThat(done.getFinishedAt()).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void incrementalSync_shouldMarkFailedAndRethrowOnError() {
        when(dataSourceProvider.active()).thenReturn(new StubJwxtDataSource());
        when(syncLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>(1, 1));
        doAnswer(inv -> {
            ((JwxtSyncLog) inv.getArgument(0)).setId(7L);
            return 1;
        }).when(syncLogMapper).insert(any(JwxtSyncLog.class));
        when(rawDataService.stage(anyList())).thenThrow(new RuntimeException("DB down"));

        try {
            service.incrementalSync("SCHEDULE");
        } catch (RuntimeException expected) {
            assertThat(expected.getMessage()).isEqualTo("DB down");
        }

        ArgumentCaptor<JwxtSyncLog> failedCaptor = ArgumentCaptor.forClass(JwxtSyncLog.class);
        verify(syncLogMapper).updateById(failedCaptor.capture());
        JwxtSyncLog failed = failedCaptor.getValue();
        assertThat(failed.getStatus()).isEqualTo(3);            // 失败
        assertThat(failed.getErrorMsg()).isEqualTo("DB down");
    }

    @Test
    @SuppressWarnings("unchecked")
    void incrementalSync_shouldUseLastSuccessfulSyncDateAsSince() {
        when(dataSourceProvider.active()).thenReturn(new StubJwxtDataSource());
        JwxtSyncLog last = JwxtSyncLog.builder()
                .id(1L).status(1).syncDate(java.time.LocalDate.of(2026, 6, 20)).build();
        Page<JwxtSyncLog> page = new Page<>(1, 1);
        page.setRecords(List.of(last));
        when(syncLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        doAnswer(inv -> {
            ((JwxtSyncLog) inv.getArgument(0)).setId(2L);
            return 1;
        }).when(syncLogMapper).insert(any(JwxtSyncLog.class));
        when(rawDataService.stage(anyList())).thenReturn(2);

        service.incrementalSync("SCHEDULE");

        // since 取自上次成功同步日期 → 体现在 stub 的 rawJson 中（4 类型均含该日期）
        ArgumentCaptor<List<JwxtRawData>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(rawDataService, times(4)).stage(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue().get(0).getRawJson()).contains("2026-06-20");
    }

    @Test
    void incrementalSync_shouldInsertRunningLogFirst() {
        when(dataSourceProvider.active()).thenReturn(new StubJwxtDataSource());
        when(syncLogMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 1));
        doAnswer(inv -> {
            ((JwxtSyncLog) inv.getArgument(0)).setId(5L);
            return 1;
        }).when(syncLogMapper).insert(any(JwxtSyncLog.class));
        when(rawDataService.stage(anyList())).thenReturn(2);

        service.incrementalSync("MANUAL");

        ArgumentCaptor<JwxtSyncLog> insertCaptor = ArgumentCaptor.forClass(JwxtSyncLog.class);
        verify(syncLogMapper).insert(insertCaptor.capture());
        JwxtSyncLog running = insertCaptor.getValue();
        assertThat(running.getSyncType()).isEqualTo("INCREMENTAL");
        assertThat(running.getStatus()).isEqualTo(0);           // 进行中
        assertThat(running.getTriggeredBy()).isEqualTo("MANUAL");
    }
}
