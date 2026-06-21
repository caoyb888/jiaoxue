package cn.smu.edu.exam.job;

import cn.smu.edu.exam.service.MonitorService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 心跳超时检测任务（每60秒执行）。
 * last_heartbeat_at 超过90秒未更新的 ANSWERING/VERIFYING 学生标记为 OFFLINE。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatTimeoutJob {

    private final MonitorService monitorService;

    @XxlJob("heartbeatTimeoutHandler")
    public void detectOffline() {
        log.info("开始检测心跳超时学生...");
        monitorService.detectOfflineStudents();
        log.info("心跳超时检测完成");
    }
}
