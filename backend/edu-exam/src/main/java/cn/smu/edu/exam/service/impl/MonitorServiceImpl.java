package cn.smu.edu.exam.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.exam.domain.dto.ExamMonitorEventDTO;
import cn.smu.edu.exam.domain.entity.ExamMonitor;
import cn.smu.edu.exam.domain.entity.ExamPublish;
import cn.smu.edu.exam.domain.vo.HeartbeatVO;
import cn.smu.edu.exam.event.ExamAbnormalEvent;
import cn.smu.edu.exam.repository.ExamMonitorMapper;
import cn.smu.edu.exam.repository.ExamPublishMapper;
import cn.smu.edu.exam.service.MonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {

    public static final String TOPIC_ABNORMAL = "edu.exam.abnormal";
    public static final int TAB_SWITCH_WARN_THRESHOLD  = 1; // 切屏1次即告警
    public static final int TAB_SWITCH_BLOCK_THRESHOLD = 3; // 切屏3次标记 ABNORMAL
    public static final int HEARTBEAT_TIMEOUT_SECONDS  = 90;

    private final ExamMonitorMapper monitorMapper;
    private final ExamPublishMapper publishMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ── 心跳 ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public HeartbeatVO heartbeat(Long publishId, Long studentId) {
        ExamMonitor monitor = monitorMapper.selectByPublishAndStudent(publishId, studentId);
        if (monitor == null) {
            throw new BizException(ErrorCode.EXAM_SESSION_NOT_FOUND);
        }
        if ("SUBMITTED".equals(monitor.getSessionStatus())) {
            HeartbeatVO vo = new HeartbeatVO();
            vo.setSessionStatus("SUBMITTED");
            vo.setLastHeartbeatAt(monitor.getLastHeartbeatAt());
            return vo;
        }

        monitor.setLastHeartbeatAt(LocalDateTime.now());
        // 断网重连：OFFLINE 恢复为 ANSWERING
        if ("OFFLINE".equals(monitor.getSessionStatus())) {
            monitor.setSessionStatus("ANSWERING");
            log.info("学生重新上线: publishId={}, studentId={}", publishId, studentId);
        }
        monitorMapper.updateById(monitor);

        HeartbeatVO vo = new HeartbeatVO();
        vo.setSessionStatus(monitor.getSessionStatus());
        vo.setLastHeartbeatAt(monitor.getLastHeartbeatAt());
        return vo;
    }

    // ── 异常事件上报 ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void reportEvent(Long publishId, Long studentId, ExamMonitorEventDTO dto) {
        ExamMonitor monitor = monitorMapper.selectByPublishAndStudent(publishId, studentId);
        if (monitor == null) {
            throw new BizException(ErrorCode.EXAM_SESSION_NOT_FOUND);
        }
        if ("SUBMITTED".equals(monitor.getSessionStatus())) {
            return; // 已交卷，忽略
        }

        String eventType = dto.getEventType().toUpperCase();
        boolean shouldAlert = false;

        switch (eventType) {
            case "TAB_SWITCH" -> {
                int count = safeInc(monitor.getTabSwitchCount());
                monitor.setTabSwitchCount(count);
                if (count >= TAB_SWITCH_BLOCK_THRESHOLD) {
                    monitor.setAbnormalFlag(1);
                    monitor.setSessionStatus("ABNORMAL");
                    log.warn("切屏次数超限，标记异常: publishId={}, studentId={}, count={}", publishId, studentId, count);
                }
                shouldAlert = count >= TAB_SWITCH_WARN_THRESHOLD;
            }
            case "SCREENSHOT" -> {
                monitor.setScreenshotCount(safeInc(monitor.getScreenshotCount()));
                monitor.setAbnormalFlag(1);
                shouldAlert = true;
            }
            case "COPY" -> {
                monitor.setCopyCount(safeInc(monitor.getCopyCount()));
                ExamPublish publish = publishMapper.selectById(publishId);
                shouldAlert = publish == null || !Integer.valueOf(1).equals(publish.getAllowCopy());
            }
            default -> log.warn("未知监考事件类型: {}", eventType);
        }

        monitorMapper.updateById(monitor);

        if (shouldAlert) {
            int count = switch (eventType) {
                case "TAB_SWITCH"  -> monitor.getTabSwitchCount();
                case "SCREENSHOT"  -> monitor.getScreenshotCount();
                case "COPY"        -> monitor.getCopyCount();
                default            -> 0;
            };
            ExamAbnormalEvent event = new ExamAbnormalEvent(
                    publishId, studentId, eventType, count,
                    "ABNORMAL".equals(monitor.getSessionStatus()), LocalDateTime.now());
            kafkaTemplate.send(TOPIC_ABNORMAL, String.valueOf(publishId), event);
            log.info("发送监考告警: publishId={}, studentId={}, type={}, count={}",
                    publishId, studentId, eventType, count);
        }
    }

    // ── 心跳超时检测（XXL-Job） ───────────────────────────────────────────────

    @Override
    @Transactional
    public void detectOfflineStudents() {
        List<ExamMonitor> stale = monitorMapper.selectStaleHeartbeats(HEARTBEAT_TIMEOUT_SECONDS);
        if (stale.isEmpty()) return;

        for (ExamMonitor m : stale) {
            m.setSessionStatus("OFFLINE");
            monitorMapper.updateById(m);
        }
        log.info("心跳超时检测: 标记 {} 名学生为 OFFLINE", stale.size());
    }

    private int safeInc(Integer val) {
        return val == null ? 1 : val + 1;
    }
}
