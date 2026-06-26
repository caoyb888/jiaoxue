package cn.smu.edu.jwxt.service.impl;

import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.common.event.NotifyEvent;
import cn.smu.edu.jwxt.service.JwxtFullSyncService;
import cn.smu.edu.jwxt.service.JwxtSyncExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

/**
 * {@link JwxtFullSyncService} 实现，委托 {@link JwxtSyncExecutor}：
 * 类型 FULL、增量基准取很早的日期以拉取全部、成功后向触发管理员发通知。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwxtFullSyncServiceImpl implements JwxtFullSyncService {

    /** 全量同步通知类型（edu-notify NoticeConsumer 单播给触发管理员）。 */
    static final String NOTIFY_TYPE = "JWXT_SYNC_DONE";
    /** 全量基准：足够早，拉取全部历史数据。 */
    private static final LocalDate FULL_SINCE = LocalDate.of(2000, 1, 1);

    private final JwxtSyncExecutor syncExecutor;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public long fullSync(Long operatorId) {
        return syncExecutor.runSync("FULL", FULL_SINCE, "MANUAL", logId -> notifyOperator(operatorId, logId));
    }

    /** 旁路通知：发送失败不影响同步结果，吞掉异常仅告警。 */
    private void notifyOperator(Long operatorId, long syncLogId) {
        if (operatorId == null) {
            log.warn("全量同步完成但无触发管理员ID，跳过通知: syncLogId={}", syncLogId);
            return;
        }
        try {
            NotifyEvent event = NotifyEvent.toUser(operatorId, NOTIFY_TYPE,
                    "教务全量同步已完成", Map.of("syncLogId", syncLogId));
            kafkaTemplate.send(KafkaTopic.NOTICE, event);
            log.info("已发送教务全量同步完成通知: operatorId={}, syncLogId={}", operatorId, syncLogId);
        } catch (Exception e) {
            log.error("发送教务全量同步完成通知失败: operatorId={}, syncLogId={}", operatorId, syncLogId, e);
        }
    }
}
