package cn.smu.edu.notify.service.impl;

import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.common.event.NoticePublishEvent;
import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.notify.domain.dto.NoticePublishDTO;
import cn.smu.edu.notify.domain.entity.Notice;
import cn.smu.edu.notify.domain.vo.NoticeVO;
import cn.smu.edu.notify.repository.NoticeMapper;
import cn.smu.edu.notify.service.NoticeTargetResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceImplTest {

    @Mock
    NoticeMapper noticeMapper;
    @Mock
    cn.smu.edu.notify.repository.NoticeQueryMapper noticeQueryMapper;
    @Mock
    NoticeTargetResolver targetResolver;
    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;

    private NoticeServiceImpl service() {
        return new NoticeServiceImpl(noticeMapper, noticeQueryMapper, targetResolver, kafkaTemplate);
    }

    private NoticePublishDTO dto(String scope) {
        NoticePublishDTO d = new NoticePublishDTO();
        d.setTitle("期末考试安排");
        d.setContent("<p>详见附件</p>");
        d.setScope(scope);
        d.setTargetRoles("ALL");
        return d;
    }

    @Test
    void publish_school_shouldInsertSetSendCountAndSendKafka() {
        when(noticeMapper.insert(any(Notice.class))).thenAnswer(inv -> {
            inv.getArgument(0, Notice.class).setId(99L);
            return 1;
        });
        when(targetResolver.resolve(any(Notice.class))).thenReturn(List.of(1L, 2L, 3L));

        NoticeVO vo = service().publish(7L, "teacher01", dto("SCHOOL"));

        assertThat(vo.getId()).isEqualTo(99L);
        assertThat(vo.getSendCount()).isEqualTo(3);
        assertThat(vo.getStatus()).isEqualTo(2);          // 已发布
        assertThat(vo.getSenderName()).isEqualTo("teacher01"); // 未自定义则取用户名

        // send_count 回写
        ArgumentCaptor<Notice> upd = ArgumentCaptor.forClass(Notice.class);
        verify(noticeMapper).updateById(upd.capture());
        assertThat(upd.getValue().getSendCount()).isEqualTo(3);

        // Kafka 仅携带 noticeId
        ArgumentCaptor<NoticePublishEvent> evt = ArgumentCaptor.forClass(NoticePublishEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopic.NOTICE_PUBLISH), eq("99"), evt.capture());
        assertThat(evt.getValue().getNoticeId()).isEqualTo(99L);
        assertThat(evt.getValue().getScope()).isEqualTo("SCHOOL");
    }

    @Test
    void publish_customSenderName_shouldBeUsed() {
        when(noticeMapper.insert(any(Notice.class))).thenReturn(1);
        when(targetResolver.resolve(any(Notice.class))).thenReturn(List.of());
        NoticePublishDTO d = dto("SCHOOL");
        d.setSenderName("教务处");

        NoticeVO vo = service().publish(7L, "teacher01", d);

        assertThat(vo.getSenderName()).isEqualTo("教务处");
        assertThat(vo.getSendCount()).isZero();
    }

    @Test
    void publish_deptWithoutDeptId_shouldThrowAndNotInsert() {
        assertThatThrownBy(() -> service().publish(7L, "t", dto("DEPT")))
                .isInstanceOf(BizException.class);
        verify(noticeMapper, never()).insert(any(Notice.class));
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void publish_classWithoutClassId_shouldThrow() {
        assertThatThrownBy(() -> service().publish(7L, "t", dto("CLASS")))
                .isInstanceOf(BizException.class);
    }

    @Test
    void myNotices_shouldCapLimitAt100() {
        service().myNotices(1L, false, 500);
        verify(noticeQueryMapper).selectMyNotices(1L, false, 100);
    }

    @Test
    void myNotices_nonPositiveLimit_shouldUseDefaultMax() {
        service().myNotices(1L, true, 0);
        verify(noticeQueryMapper).selectMyNotices(1L, true, 100);
    }

    @Test
    void markRead_firstTime_shouldIncrementReadCount() {
        org.mockito.Mockito.when(noticeQueryMapper.insertReadIgnore(9L, 1L)).thenReturn(1);

        boolean first = service().markRead(9L, 1L);

        assertThat(first).isTrue();
        verify(noticeQueryMapper).incrementReadCount(9L);
    }

    @Test
    void markRead_duplicate_shouldNotIncrement() {
        org.mockito.Mockito.when(noticeQueryMapper.insertReadIgnore(9L, 1L)).thenReturn(0);

        boolean first = service().markRead(9L, 1L);

        assertThat(first).isFalse();
        verify(noticeQueryMapper, never()).incrementReadCount(any());
    }
}
