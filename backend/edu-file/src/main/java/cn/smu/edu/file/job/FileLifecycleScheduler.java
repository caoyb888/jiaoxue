package cn.smu.edu.file.job;

import cn.smu.edu.file.service.FileLifecycleService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文件生命周期定时任务（S8-06），建议每日 03:00 在 18160 admin 配置 cron 触发。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileLifecycleScheduler {

    private final FileLifecycleService fileLifecycleService;

    @XxlJob("fileLifecycleCheck")
    public void fileLifecycleCheck() {
        log.info("开始执行文件生命周期检查...");
        FileLifecycleService.Result r = fileLifecycleService.runLifecycleCheck();
        XxlJobHelper.handleSuccess("文件生命周期完成：转冷存储 " + r.coldCount() + "，删除 " + r.deletedCount());
    }
}
