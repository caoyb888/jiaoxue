package cn.smu.edu.file.job;

import cn.smu.edu.file.service.FileLifecycleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileLifecycleSchedulerTest {

    @Mock
    FileLifecycleService fileLifecycleService;
    @InjectMocks
    FileLifecycleScheduler scheduler;

    @Test
    void fileLifecycleCheck_shouldDelegateToService() {
        when(fileLifecycleService.runLifecycleCheck())
                .thenReturn(new FileLifecycleService.Result(3, 1));

        scheduler.fileLifecycleCheck();

        verify(fileLifecycleService).runLifecycleCheck();
    }
}
