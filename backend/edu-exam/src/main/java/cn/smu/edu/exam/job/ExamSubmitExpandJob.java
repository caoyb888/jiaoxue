package cn.smu.edu.exam.job;

import cn.smu.edu.exam.service.SubmitService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * C2 第三层：XXL-Job 每30秒扫描 exam_submit_queue（process_status=0），
 * 批量展开到 student_answer 并自动批改客观题。
 * 每批最多200条，防止单次执行时间过长。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamSubmitExpandJob {

    private final SubmitService submitService;

    @XxlJob("examSubmitExpandHandler")
    public void expandSubmitQueue() {
        log.info("开始展开交卷队列...");
        submitService.expandSubmitQueue(200);
        log.info("交卷队列展开完成");
    }
}
