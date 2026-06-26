package cn.smu.edu.jwxt.service.impl;

import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.common.event.NotifyEvent;
import cn.smu.edu.jwxt.service.JwxtSyncExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.function.LongConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwxtFullSyncServiceImplTest {

    @Mock
    JwxtSyncExecutor syncExecutor;
    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;

    /** 让 executor.runSync 立即用给定 logId 触发 onSuccess 回调，模拟同步成功。 */
    private void stubExecutorSuccess(long logId) {
        when(syncExecutor.runSync(eq("FULL"), any(LocalDate.class), eq("MANUAL"), any()))
                .thenAnswer(inv -> {
                    LongConsumer onSuccess = inv.getArgument(3);
                    if (onSuccess != null) {
                        onSuccess.accept(logId);
                    }
                    return logId;
                });
    }

    @Test
    void fullSync_shouldDelegateFullAndNotifyOperatorOnSuccess() {
        stubExecutorSuccess(123L);

        JwxtFullSyncServiceImpl service = new JwxtFullSyncServiceImpl(syncExecutor, kafkaTemplate);
        long logId = service.fullSync(999L);

        assertThat(logId).isEqualTo(123L);

        ArgumentCaptor<NotifyEvent> evCaptor = ArgumentCaptor.forClass(NotifyEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopic.NOTICE), evCaptor.capture());
        NotifyEvent ev = evCaptor.getValue();
        assertThat(ev.getUserId()).isEqualTo(999L);
        assertThat(ev.getType()).isEqualTo("JWXT_SYNC_DONE");
        assertThat(ev.getPayload()).containsEntry("syncLogId", 123L);
    }

    @Test
    void fullSync_shouldSkipNotifyWhenNoOperator() {
        stubExecutorSuccess(50L);

        JwxtFullSyncServiceImpl service = new JwxtFullSyncServiceImpl(syncExecutor, kafkaTemplate);
        service.fullSync(null);

        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    void fullSync_shouldNotFailWhenNotifyThrows() {
        stubExecutorSuccess(77L);
        when(kafkaTemplate.send(any(), any())).thenThrow(new RuntimeException("kafka down"));

        JwxtFullSyncServiceImpl service = new JwxtFullSyncServiceImpl(syncExecutor, kafkaTemplate);
        // 通知失败被吞掉，全量同步仍返回成功的 logId
        assertThat(service.fullSync(1L)).isEqualTo(77L);
    }
}
