package cn.smu.edu.exam.job;

import cn.smu.edu.exam.service.ExamPublishService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * XXL-Job：每分钟同步 exam_publish.status 冗余字段（0未开始/1进行中/2已结束）。
 * 由实时计算结果写回 DB，减少查询时计算压力，供监控大屏直读。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamStatusSyncJob {

    private final ExamPublishService examPublishService;

    @XxlJob("examStatusSyncHandler")
    public void syncExamStatus() {
        log.info("开始同步考试状态...");
        examPublishService.syncExamStatus();
        log.info("考试状态同步完成");
    }
}
