package cn.smu.edu.jwxt.job;

import cn.smu.edu.jwxt.service.JwxtIncrementalSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwxtSyncJobTest {

    @Mock
    JwxtIncrementalSyncService incrementalSyncService;

    @InjectMocks
    JwxtSyncJob job;

    @Test
    void jwxtIncrementalSync_shouldTriggerScheduleSync() {
        when(incrementalSyncService.incrementalSync("SCHEDULE")).thenReturn(42L);

        job.jwxtIncrementalSync();

        verify(incrementalSyncService).incrementalSync("SCHEDULE");
    }
}
