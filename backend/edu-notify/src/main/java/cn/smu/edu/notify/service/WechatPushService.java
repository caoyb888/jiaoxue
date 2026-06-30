package cn.smu.edu.notify.service;

import cn.smu.edu.notify.domain.entity.Notice;

import java.util.List;

/**
 * 微信订阅消息批量推送（S8-10）。dev 为 mock，生产对接微信订阅消息模板 API（OPS）。
 */
public interface WechatPushService {

    /**
     * 向目标用户批量推送通知订阅消息（按 openId 分块）。
     *
     * @return 实际推送的订阅消息条数（已绑定微信的用户数）
     */
    int pushNotice(Notice notice, List<Long> userIds);
}
