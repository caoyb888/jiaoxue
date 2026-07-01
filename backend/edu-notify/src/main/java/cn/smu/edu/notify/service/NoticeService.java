package cn.smu.edu.notify.service;

import cn.smu.edu.notify.domain.dto.NoticePublishDTO;
import cn.smu.edu.notify.domain.vo.NoticeItemVO;
import cn.smu.edu.notify.domain.vo.NoticeVO;

import java.util.List;

/**
 * 通知公告发布（S8-10）+ 接收端查询（S8-15）。
 */
public interface NoticeService {

    /** 发布通知（直接发布，status=已发布），返回含发送人数的通知。 */
    NoticeVO publish(Long senderId, String username, NoticePublishDTO dto);

    /** 按 ID 查询通知。 */
    NoticeVO getById(Long noticeId);

    /** 当前用户可见的通知列表（含已读标记）；onlyUnread 时仅未读。 */
    List<NoticeItemVO> myNotices(Long userId, boolean onlyUnread, int limit);

    /** 当前用户未读通知数。 */
    long unreadCount(Long userId);

    /** 标记通知已读（幂等）；首次已读返回 true 并累加 read_count。 */
    boolean markRead(Long noticeId, Long userId);
}
