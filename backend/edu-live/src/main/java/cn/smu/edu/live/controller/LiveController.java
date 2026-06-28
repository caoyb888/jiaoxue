package cn.smu.edu.live.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.live.domain.vo.LiveConfigVO;
import cn.smu.edu.live.domain.vo.ReplayVO;
import cn.smu.edu.live.service.LiveService;
import cn.smu.edu.live.service.ReplayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课堂直播 API（S8-01）。角色鉴权由网关统一处理（教师）。
 *
 * <p>C5：SLIDE_ONLY 线下课堂返回的配置中 webrtcEnabled/rtmpEnabled 均为 false、
 * 推/拉流地址为 null，前端据此不发起任何 WebRTC 连接。
 */
@RestController
@RequestMapping("/api/v1/live")
@RequiredArgsConstructor
public class LiveController {

    private final LiveService liveService;
    private final ReplayService replayService;

    /** 开启课堂直播（按 live_mode 分级）。 */
    @PostMapping("/{lessonId}/start")
    @OperationLog(module = "live", operation = "开启课堂直播")
    public Result<LiveConfigVO> start(@PathVariable Long lessonId) {
        return Result.ok(liveService.startLive(lessonId, UserContext.getUserId()));
    }

    /** 查询课堂直播配置。 */
    @GetMapping("/{lessonId}")
    public Result<LiveConfigVO> config(@PathVariable Long lessonId) {
        return Result.ok(liveService.getLiveConfig(lessonId));
    }

    /** 结束课堂直播。 */
    @PostMapping("/{lessonId}/stop")
    @OperationLog(module = "live", operation = "结束课堂直播")
    public Result<LiveConfigVO> stop(@PathVariable Long lessonId) {
        return Result.ok(liveService.stopLive(lessonId, UserContext.getUserId()));
    }

    /** 获取课堂回放（replay_visible 控制学生可见性）。 */
    @GetMapping("/{lessonId}/replay")
    public Result<ReplayVO> replay(@PathVariable Long lessonId) {
        return Result.ok(replayService.getReplay(lessonId, UserContext.getRoles()));
    }
}
