package cn.smu.edu.notify.service.impl;

import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.common.event.NoticePublishEvent;
import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.notify.domain.dto.NoticePublishDTO;
import cn.smu.edu.notify.domain.entity.Notice;
import cn.smu.edu.notify.domain.vo.NoticeItemVO;
import cn.smu.edu.notify.domain.vo.NoticeVO;
import cn.smu.edu.notify.repository.NoticeMapper;
import cn.smu.edu.notify.repository.NoticeQueryMapper;
import cn.smu.edu.notify.service.NoticeService;
import cn.smu.edu.notify.service.NoticeTargetResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/** {@link NoticeService} 实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private static final int STATUS_PUBLISHED = 2;

    /** myNotices 默认返回上限。 */
    private static final int MAX_LIMIT = 100;

    private final NoticeMapper noticeMapper;
    private final NoticeQueryMapper noticeQueryMapper;
    private final NoticeTargetResolver targetResolver;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public NoticeVO publish(Long senderId, String username, NoticePublishDTO dto) {
        validateScope(dto);

        Notice notice = Notice.builder()
                .senderId(senderId)
                .senderName(StringUtils.hasText(dto.getSenderName()) ? dto.getSenderName() : username)
                .title(dto.getTitle())
                .content(dto.getContent())
                .scope(dto.getScope())
                .deptId("DEPT".equals(dto.getScope()) ? dto.getDeptId() : null)
                .classId("CLASS".equals(dto.getScope()) ? dto.getClassId() : null)
                .targetRoles(StringUtils.hasText(dto.getTargetRoles()) ? dto.getTargetRoles() : "ALL")
                .needReview(0)
                .status(STATUS_PUBLISHED)
                .readCount(0)
                .publishedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        noticeMapper.insert(notice);

        List<Long> targets = targetResolver.resolve(notice);
        notice.setSendCount(targets.size());
        Notice countUpdate = new Notice();
        countUpdate.setId(notice.getId());
        countUpdate.setSendCount(targets.size());
        noticeMapper.updateById(countUpdate);

        // 异步批量微信订阅推送（消费者据 noticeId 回查并解析收件人）
        kafkaTemplate.send(KafkaTopic.NOTICE_PUBLISH, String.valueOf(notice.getId()),
                NoticePublishEvent.builder()
                        .noticeId(notice.getId()).title(notice.getTitle()).scope(notice.getScope()).build());

        log.info("通知发布: noticeId={}, scope={}, 目标人数={}, sender={}",
                notice.getId(), notice.getScope(), targets.size(), senderId);
        return NoticeVO.from(notice);
    }

    @Override
    public NoticeVO getById(Long noticeId) {
        Notice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "通知不存在: " + noticeId);
        }
        return NoticeVO.from(notice);
    }

    @Override
    public List<NoticeItemVO> myNotices(Long userId, boolean onlyUnread, int limit) {
        int capped = limit <= 0 ? MAX_LIMIT : Math.min(limit, MAX_LIMIT);
        return noticeQueryMapper.selectMyNotices(userId, onlyUnread, capped);
    }

    @Override
    public long unreadCount(Long userId) {
        return noticeQueryMapper.countUnread(userId);
    }

    @Override
    @Transactional
    public boolean markRead(Long noticeId, Long userId) {
        boolean firstRead = noticeQueryMapper.insertReadIgnore(noticeId, userId) > 0;
        if (firstRead) {
            noticeQueryMapper.incrementReadCount(noticeId);
        }
        return firstRead;
    }

    private void validateScope(NoticePublishDTO dto) {
        if ("DEPT".equals(dto.getScope()) && dto.getDeptId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "院系范围必须指定 deptId");
        }
        if ("CLASS".equals(dto.getScope()) && dto.getClassId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "班级范围必须指定 classId");
        }
    }
}
