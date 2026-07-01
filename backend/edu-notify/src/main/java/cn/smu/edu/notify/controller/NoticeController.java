package cn.smu.edu.notify.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.notify.domain.dto.NoticePublishDTO;
import cn.smu.edu.notify.domain.vo.NoticeItemVO;
import cn.smu.edu.notify.domain.vo.NoticeVO;
import cn.smu.edu.notify.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通知公告发布 API（S8-10）。角色鉴权由网关统一处理（教师/管理员）。
 */
@RestController
@RequestMapping("/api/v1/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /** 发布通知（全校/院系/班级），异步批量微信订阅推送。 */
    @OperationLog(module = "notify", operation = "发布通知公告")
    @PostMapping("/publish")
    public Result<NoticeVO> publish(@Valid @RequestBody NoticePublishDTO dto) {
        return Result.ok(noticeService.publish(
                UserContext.getUserId(), UserContext.getUsername(), dto));
    }

    /** 当前用户可见的通知列表（接收端），onlyUnread=true 仅未读。 */
    @GetMapping("/my")
    public Result<List<NoticeItemVO>> myNotices(
            @RequestParam(defaultValue = "false") boolean onlyUnread,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.ok(noticeService.myNotices(UserContext.getUserId(), onlyUnread, limit));
    }

    /** 当前用户未读通知数（用于未读 Badge）。 */
    @GetMapping("/my/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(noticeService.unreadCount(UserContext.getUserId()));
    }

    /** 标记通知已读。 */
    @PostMapping("/{noticeId}/read")
    public Result<Void> markRead(@PathVariable Long noticeId) {
        noticeService.markRead(noticeId, UserContext.getUserId());
        return Result.ok();
    }

    /** 查询通知详情。 */
    @GetMapping("/{noticeId}")
    public Result<NoticeVO> get(@PathVariable Long noticeId) {
        return Result.ok(noticeService.getById(noticeId));
    }
}
