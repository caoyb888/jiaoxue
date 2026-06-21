package cn.smu.edu.exam.service;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.exam.domain.dto.ExamMonitorEventDTO;
import cn.smu.edu.exam.domain.entity.ExamMonitor;
import cn.smu.edu.exam.domain.entity.ExamPublish;
import cn.smu.edu.exam.domain.vo.HeartbeatVO;
import cn.smu.edu.exam.event.ExamAbnormalEvent;
import cn.smu.edu.exam.repository.ExamMonitorMapper;
import cn.smu.edu.exam.repository.ExamPublishMapper;
import cn.smu.edu.exam.service.impl.MonitorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MonitorServiceTest {

    @Mock private ExamMonitorMapper monitorMapper;
    @Mock private ExamPublishMapper publishMapper;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks private MonitorServiceImpl service;

    private ExamMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new ExamMonitor();
        monitor.setId(1L);
        monitor.setPublishId(10L);
        monitor.setStudentId(99L);
        monitor.setSessionStatus("ANSWERING");
        monitor.setTabSwitchCount(0);
        monitor.setScreenshotCount(0);
        monitor.setCopyCount(0);
        monitor.setLastHeartbeatAt(LocalDateTime.now().minusSeconds(10));
    }

    // ── 心跳 ──────────────────────────────────────────────────────────────────

    @Test
    void heartbeat_shouldUpdateLastHeartbeatAt() {
        when(monitorMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(monitor);
        HeartbeatVO vo = service.heartbeat(10L, 99L);
        assertThat(vo.getSessionStatus()).isEqualTo("ANSWERING");
        assertThat(vo.getLastHeartbeatAt()).isNotNull();
        verify(monitorMapper).updateById((ExamMonitor) monitor);
    }

    @Test
    void heartbeat_shouldRestoreToAnswering_whenWasOffline() {
        monitor.setSessionStatus("OFFLINE");
        when(monitorMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(monitor);
        HeartbeatVO vo = service.heartbeat(10L, 99L);
        assertThat(vo.getSessionStatus()).isEqualTo("ANSWERING");
        assertThat(monitor.getSessionStatus()).isEqualTo("ANSWERING");
    }

    @Test
    void heartbeat_shouldReturnSubmittedWithoutUpdate_whenAlreadySubmitted() {
        monitor.setSessionStatus("SUBMITTED");
        when(monitorMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(monitor);
        HeartbeatVO vo = service.heartbeat(10L, 99L);
        assertThat(vo.getSessionStatus()).isEqualTo("SUBMITTED");
        verify(monitorMapper, never()).updateById((ExamMonitor) any());
    }

    @Test
    void heartbeat_shouldThrow_whenMonitorNotFound() {
        when(monitorMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(null);
        assertThatThrownBy(() -> service.heartbeat(10L, 99L))
                .isInstanceOf(BizException.class);
    }

    // ── 切屏事件 ──────────────────────────────────────────────────────────────

    @Test
    void reportEvent_tabSwitch_shouldIncrementCountAndSendAlert() {
        when(monitorMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(monitor);
        ExamMonitorEventDTO dto = buildEvent("TAB_SWITCH");
        service.reportEvent(10L, 99L, dto);
        assertThat(monitor.getTabSwitchCount()).isEqualTo(1);
        assertThat(monitor.getSessionStatus()).isEqualTo("ANSWERING"); // 未超阈值
        verify(kafkaTemplate).send(eq("edu.exam.abnormal"), eq("10"), any(ExamAbnormalEvent.class));
    }

    @Test
    void reportEvent_tabSwitch_shouldMarkAbnormal_whenThresholdExceeded() {
        monitor.setTabSwitchCount(2); // 已有2次，再来1次=3次
        when(monitorMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(monitor);
        service.reportEvent(10L, 99L, buildEvent("TAB_SWITCH"));
        assertThat(monitor.getTabSwitchCount()).isEqualTo(3);
        assertThat(monitor.getSessionStatus()).isEqualTo("ABNORMAL");
        assertThat(monitor.getAbnormalFlag()).isEqualTo(1);
        verify(kafkaTemplate).send(eq("edu.exam.abnormal"), any(), any());
    }

    // ── 截图事件 ──────────────────────────────────────────────────────────────

    @Test
    void reportEvent_screenshot_shouldSetAbnormalFlagAndSendAlert() {
        when(monitorMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(monitor);
        service.reportEvent(10L, 99L, buildEvent("SCREENSHOT"));
        assertThat(monitor.getScreenshotCount()).isEqualTo(1);
        assertThat(monitor.getAbnormalFlag()).isEqualTo(1);

        ArgumentCaptor<ExamAbnormalEvent> cap = ArgumentCaptor.forClass(ExamAbnormalEvent.class);
        verify(kafkaTemplate).send(eq("edu.exam.abnormal"), eq("10"), cap.capture());
        assertThat(cap.getValue().getEventType()).isEqualTo("SCREENSHOT");
    }

    // ── 复制事件 ──────────────────────────────────────────────────────────────

    @Test
    void reportEvent_copy_shouldSendAlert_whenCopyNotAllowed() {
        ExamPublish pub = new ExamPublish();
        pub.setAllowCopy(0);
        when(monitorMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(monitor);
        when(publishMapper.selectById(10L)).thenReturn(pub);
        service.reportEvent(10L, 99L, buildEvent("COPY"));
        assertThat(monitor.getCopyCount()).isEqualTo(1);
        verify(kafkaTemplate).send(eq("edu.exam.abnormal"), any(), any());
    }

    @Test
    void reportEvent_copy_shouldNotSendAlert_whenCopyAllowed() {
        ExamPublish pub = new ExamPublish();
        pub.setAllowCopy(1);
        when(monitorMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(monitor);
        when(publishMapper.selectById(10L)).thenReturn(pub);
        service.reportEvent(10L, 99L, buildEvent("COPY"));
        assertThat(monitor.getCopyCount()).isEqualTo(1);
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void reportEvent_shouldIgnore_whenAlreadySubmitted() {
        monitor.setSessionStatus("SUBMITTED");
        when(monitorMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(monitor);
        service.reportEvent(10L, 99L, buildEvent("TAB_SWITCH"));
        verify(monitorMapper, never()).updateById((ExamMonitor) any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    // ── 离线检测 ──────────────────────────────────────────────────────────────

    @Test
    void detectOfflineStudents_shouldMarkOffline_whenHeartbeatStale() {
        ExamMonitor stale = new ExamMonitor();
        stale.setId(2L);
        stale.setSessionStatus("ANSWERING");
        stale.setLastHeartbeatAt(LocalDateTime.now().minusSeconds(120));
        when(monitorMapper.selectStaleHeartbeats(MonitorServiceImpl.HEARTBEAT_TIMEOUT_SECONDS))
                .thenReturn(List.of(stale));

        service.detectOfflineStudents();

        assertThat(stale.getSessionStatus()).isEqualTo("OFFLINE");
        verify(monitorMapper).updateById((ExamMonitor) stale);
    }

    @Test
    void detectOfflineStudents_shouldDoNothing_whenNoStaleHeartbeats() {
        when(monitorMapper.selectStaleHeartbeats(anyInt())).thenReturn(List.of());
        service.detectOfflineStudents();
        verify(monitorMapper, never()).updateById((ExamMonitor) any());
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private ExamMonitorEventDTO buildEvent(String type) {
        ExamMonitorEventDTO dto = new ExamMonitorEventDTO();
        dto.setEventType(type);
        return dto;
    }
}
