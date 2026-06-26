package cn.smu.edu.jwxt.service.impl;

import cn.smu.edu.jwxt.service.JwxtSyncExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwxtIncrementalSyncServiceImplTest {

    @Mock
    JwxtSyncExecutor syncExecutor;

    @InjectMocks
    JwxtIncrementalSyncServiceImpl service;

    @Test
    void incrementalSync_shouldDelegateWithIncrementalTypeAndLastSyncDate() {
        LocalDate since = LocalDate.of(2026, 6, 20);
        when(syncExecutor.lastSuccessfulSyncDate()).thenReturn(since);
        when(syncExecutor.runSync(eq("INCREMENTAL"), eq(since), eq("SCHEDULE"), isNull())).thenReturn(88L);

        long logId = service.incrementalSync("SCHEDULE");

        assertThat(logId).isEqualTo(88L);
        verify(syncExecutor).runSync("INCREMENTAL", since, "SCHEDULE", null);
    }
}
