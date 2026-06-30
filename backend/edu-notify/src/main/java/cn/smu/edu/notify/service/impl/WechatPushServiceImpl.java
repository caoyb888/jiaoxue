package cn.smu.edu.notify.service.impl;

import cn.smu.edu.notify.domain.entity.Notice;
import cn.smu.edu.notify.repository.NoticeTargetMapper;
import cn.smu.edu.notify.service.WechatPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link WechatPushService} 实现：分块拉取目标用户 openId，逐块推送微信订阅消息。
 *
 * <p>dev/CI（{@code notify.wechat.mock-mode=true}，默认）仅记录日志不真发，便于联调与测试；
 * 生产由 OPS 接入微信「订阅消息」模板 API。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatPushServiceImpl implements WechatPushService {

    /** 微信订阅消息批量发送分块大小（避免单次 IN 查询过大、控制每批推送量）。 */
    private static final int BATCH_SIZE = 500;

    private final NoticeTargetMapper targetMapper;

    @Value("${notify.wechat.mock-mode:true}")
    private boolean mockMode;

    @Override
    public int pushNotice(Notice notice, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }
        int pushed = 0;
        for (int i = 0; i < userIds.size(); i += BATCH_SIZE) {
            List<Long> chunk = userIds.subList(i, Math.min(i + BATCH_SIZE, userIds.size()));
            List<String> openIds = targetMapper.selectOpenIdsByUserIds(chunk);
            if (openIds.isEmpty()) {
                continue;
            }
            sendBatch(notice, openIds);
            pushed += openIds.size();
        }
        log.info("微信订阅推送完成: noticeId={}, 目标用户={}, 实推条数={}, mock={}",
                notice.getId(), userIds.size(), pushed, mockMode);
        return pushed;
    }

    /** 推送一批订阅消息。dev mock 仅打印；生产替换为微信模板消息 API 调用。 */
    private void sendBatch(Notice notice, List<String> openIds) {
        if (mockMode) {
            log.info("[MOCK] 微信订阅消息: noticeId={}, title={}, 收件 openId 数={}",
                    notice.getId(), notice.getTitle(), openIds.size());
            return;
        }
        // 生产：调用微信订阅消息模板 API（OPS 接入），逐 openId 发送 data + page。
        log.warn("微信订阅消息真实推送未接入（OPS），noticeId={}, openId 数={}",
                notice.getId(), openIds.size());
    }
}
