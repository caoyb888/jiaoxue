package cn.smu.edu.grade.job;

import cn.smu.edu.grade.service.GradeCalcService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeCalcJobTest {

    @Mock
    GradeCalcService gradeCalcService;
    @InjectMocks
    GradeCalcJob job;

    @Test
    void gradeCalc_noParam_shouldCalculatePending() {
        // 无 XxlJobContext 时 getJobParam() 返回 null → 计算待算
        when(gradeCalcService.calculatePending()).thenReturn(5);

        job.gradeCalc();

        verify(gradeCalcService).calculatePending();
    }
}
