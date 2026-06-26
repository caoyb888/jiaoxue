package cn.smu.edu.jwxt.service;

import cn.smu.edu.jwxt.adapter.JwxtDataSourceProvider;
import cn.smu.edu.jwxt.adapter.StubJwxtDataSource;
import cn.smu.edu.jwxt.domain.entity.JwxtRawData;
import cn.smu.edu.jwxt.domain.entity.JwxtSyncLog;
import cn.smu.edu.jwxt.repository.JwxtSyncLogMapper;
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

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwxtSyncExecutorTest {

    @Mock
    JwxtDataSourceProvider dataSourceProvider;
    @Mock
    JwxtRawDataService rawDataService;
    @Mock
    JwxtMappingService mappingService;
    @Mock
    JwxtSyncLogMapper syncLogMapper;

    JwxtSyncExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new JwxtSyncExecutor(dataSourceProvider, rawDataService, mappingService, syncLogMapper);
        ReflectionTestUtils.setField(executor, "batchSize", 500);
    }

    private void stubInsertId(long id) {
        doAnswer(inv -> {
            ((JwxtSyncLog) inv.getArgument(0)).setId(id);
            return 1;
        }).when(syncLogMapper).insert(any(JwxtSyncLog.class));
    }

    @Test
    void runSync_shouldStageAllTypesCloseLogWithCountsAndCallback() {
        when(dataSourceProvider.active()).thenReturn(new StubJwxtDataSource());
        stubInsertId(99L);
        when(rawDataService.stage(anyList())).thenReturn(2);
        when(mappingService.resolveLocalId(anyString(), anyString())).thenReturn(null); // 全新增

        AtomicLong callbackLogId = new AtomicLong(-1);
        LongConsumer onSuccess = callbackLogId::set;

        long logId = executor.runSync("FULL", LocalDate.of(2000, 1, 1), "MANUAL", onSuccess);

        assertThat(logId).isEqualTo(99L);
        verify(rawDataService, times(4)).stage(anyList()); // 4 个数据类型各一批
        assertThat(callbackLogId.get()).isEqualTo(99L);    // 成功回调收到 logId

        ArgumentCaptor<JwxtSyncLog> doneCaptor = ArgumentCaptor.forClass(JwxtSyncLog.class);
        verify(syncLogMapper).updateById(doneCaptor.capture());
        JwxtSyncLog done = doneCaptor.getValue();
        assertThat(done.getStatus()).isEqualTo(1);
        assertThat(done.getStudentCnt()).isEqualTo(2);
        assertThat(done.getDeptCnt()).isEqualTo(2);
        assertThat(done.getCourseCnt()).isEqualTo(4); // COURSE 2 + CLASS 2
        assertThat(done.getSuccessCnt()).isEqualTo(8);
        assertThat(done.getFinishedAt()).isNotNull();
    }

    @Test
    void runSync_shouldMarkFailedRethrowAndSkipCallbackOnError() {
        when(dataSourceProvider.active()).thenReturn(new StubJwxtDataSource());
        stubInsertId(7L);
        when(rawDataService.stage(anyList())).thenThrow(new RuntimeException("DB down"));

        AtomicLong callbackLogId = new AtomicLong(-1);
        try {
            executor.runSync("FULL", LocalDate.of(2000, 1, 1), "MANUAL", callbackLogId::set);
        } catch (RuntimeException expected) {
            assertThat(expected.getMessage()).isEqualTo("DB down");
        }

        assertThat(callbackLogId.get()).isEqualTo(-1); // 失败不回调
        ArgumentCaptor<JwxtSyncLog> failedCaptor = ArgumentCaptor.forClass(JwxtSyncLog.class);
        verify(syncLogMapper).updateById(failedCaptor.capture());
        JwxtSyncLog failed = failedCaptor.getValue();
        assertThat(failed.getStatus()).isEqualTo(3);
        assertThat(failed.getErrorMsg()).isEqualTo("DB down");
    }

    @Test
    void runSync_shouldInsertRunningLogFirstWithGivenType() {
        when(dataSourceProvider.active()).thenReturn(new StubJwxtDataSource());
        stubInsertId(5L);
        when(rawDataService.stage(anyList())).thenReturn(2);

        executor.runSync("INCREMENTAL", LocalDate.now(), "MANUAL", null);

        ArgumentCaptor<JwxtSyncLog> insertCaptor = ArgumentCaptor.forClass(JwxtSyncLog.class);
        verify(syncLogMapper).insert(insertCaptor.capture());
        JwxtSyncLog running = insertCaptor.getValue();
        assertThat(running.getSyncType()).isEqualTo("INCREMENTAL");
        assertThat(running.getStatus()).isEqualTo(0);
        assertThat(running.getTriggeredBy()).isEqualTo("MANUAL");
    }

    @Test
    void runSync_sinceShouldFlowIntoFetch() {
        when(dataSourceProvider.active()).thenReturn(new StubJwxtDataSource());
        stubInsertId(2L);
        when(rawDataService.stage(anyList())).thenReturn(2);

        executor.runSync("INCREMENTAL", LocalDate.of(2026, 6, 20), "SCHEDULE", null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JwxtRawData>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(rawDataService, times(4)).stage(rowsCaptor.capture());
        assertThat(rowsCaptor.getValue().get(0).getRawJson()).contains("2026-06-20");
    }

    @Test
    @SuppressWarnings("unchecked")
    void lastSuccessfulSyncDate_shouldDefaultLookbackWhenNoSuccess() {
        when(syncLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>(1, 1));
        assertThat(executor.lastSuccessfulSyncDate())
                .isEqualTo(LocalDate.now().minusDays(JwxtSyncExecutor.DEFAULT_LOOKBACK_DAYS));
    }

    @Test
    @SuppressWarnings("unchecked")
    void lastSuccessfulSyncDate_shouldReturnLatestSuccessDate() {
        JwxtSyncLog last = JwxtSyncLog.builder()
                .id(1L).status(1).syncDate(LocalDate.of(2026, 6, 20)).build();
        Page<JwxtSyncLog> page = new Page<>(1, 1);
        page.setRecords(List.of(last));
        when(syncLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        assertThat(executor.lastSuccessfulSyncDate()).isEqualTo(LocalDate.of(2026, 6, 20));
    }
}
