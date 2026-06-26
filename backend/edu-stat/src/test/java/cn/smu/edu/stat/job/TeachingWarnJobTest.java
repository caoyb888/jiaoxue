package cn.smu.edu.stat.job;

import cn.smu.edu.stat.service.WarnEngineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeachingWarnJobTest {

    @Mock
    WarnEngineService warnEngineService;

    @InjectMocks
    TeachingWarnJob job;

    @Test
    void teachingWarnCheck_shouldRunCheckForToday_whenNoJobParam() {
        // 无 XxlJobContext 时 getJobParam() 返回 null → 默认当天
        when(warnEngineService.runCheck(LocalDate.now())).thenReturn(2);

        job.teachingWarnCheck();

        verify(warnEngineService).runCheck(LocalDate.now());
    }
}
