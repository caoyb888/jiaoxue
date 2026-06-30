package cn.smu.edu.notify.consumer;

import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.common.event.NoticePublishEvent;
import cn.smu.edu.notify.domain.entity.Notice;
import cn.smu.edu.notify.repository.NoticeMapper;
import cn.smu.edu.notify.service.NoticeTargetResolver;
import cn.smu.edu.notify.service.WechatPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 通知发布消费者：消费 {@code edu.notice.publish}，回查 notice → 解析收件人 → 批量微信订阅推送。
 *
 * <p>幂等：Redis 去重键 {@code notify:notice:push:{noticeId}}（TTL 24h），防重复推送（CLAUDE.md §5.6）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticePushConsumer {

    private final NoticeMapper noticeMapper;
    private final NoticeTargetResolver targetResolver;
    private final WechatPushService wechatPushService;
    private final StringRedisTemplate redisTemplate;

    @KafkaListener(topics = KafkaTopic.NOTICE_PUBLISH,
                   groupId = "notify-notice-push",
                   concurrency = "3",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consume(NoticePublishEvent event) {
        if (event == null || event.getNoticeId() == null) {
            log.warn("收到空的 NoticePublishEvent，已忽略");
            return;
        }
        String dedupeKey = "notify:notice:push:" + event.getNoticeId();
        Boolean first = redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", Duration.ofHours(24));
        if (!Boolean.TRUE.equals(first)) {
            log.info("通知推送已处理，跳过: noticeId={}", event.getNoticeId());
            return;
        }

        Notice notice = noticeMapper.selectById(event.getNoticeId());
        if (notice == null) {
            log.warn("通知不存在，跳过推送: noticeId={}", event.getNoticeId());
            return;
        }
        List<Long> targets = targetResolver.resolve(notice);
        wechatPushService.pushNotice(notice, targets);
    }
}
