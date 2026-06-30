package cn.smu.edu.notify.service;

import cn.smu.edu.notify.domain.dto.NoticePublishDTO;
import cn.smu.edu.notify.domain.vo.NoticeVO;

/**
 * 通知公告发布（S8-10）：写 notice → 解析目标人数 → 发 Kafka 异步批量微信订阅推送。
 */
public interface NoticeService {

    /** 发布通知（直接发布，status=已发布），返回含发送人数的通知。 */
    NoticeVO publish(Long senderId, String username, NoticePublishDTO dto);

    /** 按 ID 查询通知。 */
    NoticeVO getById(Long noticeId);
}
