package cn.smu.edu.live.controller;

import cn.smu.edu.live.domain.dto.SrsCallback;
import cn.smu.edu.live.service.LiveStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SRS HTTP 回调入口（S8-02）。
 *
 * <p>SRS 以 server-to-server 方式直连 edu-live（8091）回调，<b>不经网关</b>（无 JWT）。
 * SRS 约定：处理成功返回整型 {@code 0} 放行，非 0 拒绝。本服务对所有回调返回 0（旁路更新，
 * 不阻断推流）。需在 SRS 配置 http_hooks 指向本服务这些路径。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/live/srs")
@RequiredArgsConstructor
public class SrsCallbackController {

    private final LiveStreamService liveStreamService;

    @PostMapping("/on_publish")
    public int onPublish(@RequestBody SrsCallback cb) {
        liveStreamService.onPublish(cb.getStream());
        return 0;
    }

    @PostMapping("/on_unpublish")
    public int onUnpublish(@RequestBody SrsCallback cb) {
        liveStreamService.onUnpublish(cb.getStream());
        return 0;
    }

    @PostMapping("/on_dvr")
    public int onDvr(@RequestBody SrsCallback cb) {
        liveStreamService.onDvr(cb.getStream(), cb.getFile());
        return 0;
    }
}
