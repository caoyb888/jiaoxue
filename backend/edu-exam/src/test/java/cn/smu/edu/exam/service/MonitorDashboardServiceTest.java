package cn.smu.edu.exam.service;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.exam.domain.entity.ExamMonitor;
import cn.smu.edu.exam.domain.entity.ExamPublish;
import cn.smu.edu.exam.domain.vo.MonitorDashboardVO;
import cn.smu.edu.exam.repository.ExamMonitorMapper;
import cn.smu.edu.exam.repository.ExamPublishMapper;
import cn.smu.edu.exam.service.impl.MonitorDashboardServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitorDashboardServiceTest {

    @Mock private ExamPublishMapper publishMapper;
    @Mock private ExamMonitorMapper monitorMapper;

    @InjectMocks private MonitorDashboardServiceImpl service;

    @Test
    void getDashboard_shouldReturnDistributionAndStudents() {
        when(publishMapper.selectById(10L)).thenReturn(new ExamPublish());
        when(monitorMapper.selectByPublishId(10L)).thenReturn(List.of(
                monitor(99L, "ANSWERING"),
                monitor(98L, "SUBMITTED"),
                monitor(97L, "OFFLINE"),
                monitor(96L, "ABNORMAL"),
                monitor(95L, "ANSWERING")
        ));

        MonitorDashboardVO vo = service.getDashboard(10L);

        assertThat(vo.getTotalStudents()).isEqualTo(5);
        assertThat(vo.getStatusDistribution().get("ANSWERING")).isEqualTo(2L);
        assertThat(vo.getStatusDistribution().get("SUBMITTED")).isEqualTo(1L);
        assertThat(vo.getStatusDistribution().get("OFFLINE")).isEqualTo(1L);
        assertThat(vo.getStatusDistribution().get("ABNORMAL")).isEqualTo(1L);
        assertThat(vo.getStatusDistribution().get("VERIFYING")).isEqualTo(0L);
        assertThat(vo.getStudents()).hasSize(5);
    }

    @Test
    void getDashboard_shouldReturnZeroDistribution_whenNoStudents() {
        when(publishMapper.selectById(10L)).thenReturn(new ExamPublish());
        when(monitorMapper.selectByPublishId(10L)).thenReturn(List.of());

        MonitorDashboardVO vo = service.getDashboard(10L);

        assertThat(vo.getTotalStudents()).isEqualTo(0);
        assertThat(vo.getStatusDistribution().get("ANSWERING")).isEqualTo(0L);
        assertThat(vo.getStudents()).isEmpty();
    }

    @Test
    void getDashboard_shouldThrow_whenPublishNotFound() {
        when(publishMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.getDashboard(99L))
                .isInstanceOf(BizException.class);
    }

    private ExamMonitor monitor(Long studentId, String status) {
        ExamMonitor m = new ExamMonitor();
        m.setStudentId(studentId);
        m.setSessionStatus(status);
        m.setTabSwitchCount(0);
        m.setScreenshotCount(0);
        m.setCopyCount(0);
        m.setAbnormalFlag(0);
        return m;
    }
}
