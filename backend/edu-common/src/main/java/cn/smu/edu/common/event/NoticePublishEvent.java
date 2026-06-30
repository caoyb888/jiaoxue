package cn.smu.edu.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通知公告发布事件 — 生产者：edu-notify 发布接口；消费者：edu-notify NoticePushConsumer。
 * Topic: {@code edu.notice.publish}
 *
 * <p>仅携带 noticeId（轻量），消费者据此回查 notice 行重新解析目标用户并批量推送微信订阅消息，
 * 避免在消息体内塞入大批量 userId。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticePublishEvent implements Serializable {

    private Long noticeId;
    private String title;
    /** 发送范围：SCHOOL/DEPT/CLASS（仅用于日志/排查，权威以 notice 行为准）。 */
    private String scope;
}
