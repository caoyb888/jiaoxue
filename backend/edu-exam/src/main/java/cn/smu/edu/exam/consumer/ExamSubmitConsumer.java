package cn.smu.edu.exam.consumer;

import cn.smu.edu.exam.event.ExamSubmitEvent;
import cn.smu.edu.exam.service.SubmitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * C2 第二层：消费 edu.exam.submit，将答案写入 exam_submit_queue 持久化队列。
 * concurrency=10（CLAUDE.md §5.6 edu.exam.submit 并发上限）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamSubmitConsumer {

    private final SubmitService submitService;

    @KafkaListener(topics = "edu.exam.submit", concurrency = "10",
            groupId = "edu-exam-submit")
    public void consume(ExamSubmitEvent event) {
        log.info("收到交卷消息: publishId={}, studentId={}, type={}",
                event.getPublishId(), event.getStudentId(), event.getSubmitType());
        try {
            submitService.enqueueSubmit(
                    event.getPublishId(),
                    event.getStudentId(),
                    event.getAnswersJson(),
                    event.getSubmitType(),
                    event.getClientSubmitAt());
        } catch (Exception e) {
            log.error("交卷入队失败: publishId={}, studentId={}, error={}",
                    event.getPublishId(), event.getStudentId(), e.getMessage(), e);
        }
    }
}
