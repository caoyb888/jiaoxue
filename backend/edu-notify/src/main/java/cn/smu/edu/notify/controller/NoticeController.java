package cn.smu.edu.notify.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.notify.domain.dto.NoticePublishDTO;
import cn.smu.edu.notify.domain.vo.NoticeVO;
import cn.smu.edu.notify.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    /** 查询通知详情。 */
    @GetMapping("/{noticeId}")
    public Result<NoticeVO> get(@PathVariable Long noticeId) {
        return Result.ok(noticeService.getById(noticeId));
    }
}
