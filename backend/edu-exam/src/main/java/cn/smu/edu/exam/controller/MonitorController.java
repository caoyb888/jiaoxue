package cn.smu.edu.exam.controller;

import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.exam.domain.dto.ExamMonitorEventDTO;
import cn.smu.edu.exam.domain.vo.HeartbeatVO;
import cn.smu.edu.exam.service.MonitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exam/publishes")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    /**
     * 学生端心跳（前端每30秒调用一次）。
     * OFFLINE 状态时调用可恢复为 ANSWERING（断网重连）。
     */
    @PutMapping("/{publishId}/heartbeat")
    public Result<HeartbeatVO> heartbeat(@PathVariable Long publishId) {
        return Result.ok(monitorService.heartbeat(publishId, UserContext.getUserId()));
    }

    /**
     * 上报监考异常事件（切屏/截图/复制）。
     * 前端检测到事件后立即调用，edu-notify 消费 Kafka 后 WebSocket 推送教师端。
     */
    @PostMapping("/{publishId}/monitor/event")
    public Result<Void> reportEvent(
            @PathVariable Long publishId,
            @Valid @RequestBody ExamMonitorEventDTO dto) {
        monitorService.reportEvent(publishId, UserContext.getUserId(), dto);
        return Result.ok();
    }
}
