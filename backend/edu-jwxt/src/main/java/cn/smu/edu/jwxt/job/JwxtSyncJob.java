package cn.smu.edu.jwxt.job;

import cn.smu.edu.jwxt.service.JwxtIncrementalSyncService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 教务增量同步定时任务（S7-09），建议每日 02:00 在 18160 admin 配置 cron 触发。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwxtSyncJob {

    private final JwxtIncrementalSyncService incrementalSyncService;

    @XxlJob("jwxtIncrementalSync")
    public void jwxtIncrementalSync() {
        log.info("开始执行教务增量同步...");
        long logId = incrementalSyncService.incrementalSync("SCHEDULE");
        XxlJobHelper.handleSuccess("教务增量同步完成，syncLogId=" + logId);
    }
}
