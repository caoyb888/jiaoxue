package cn.smu.edu.ai.consumer;

import cn.smu.edu.ai.service.DiscussionService;
import cn.smu.edu.common.event.DiscussionMessageEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DiscussionConsumerTest {

    @Mock
    DiscussionService discussionService;
    @InjectMocks
    DiscussionConsumer consumer;

    @Test
    void consume_message_shouldAppend() {
        DiscussionMessageEvent e = DiscussionMessageEvent.builder()
                .action("MESSAGE").lessonId(1L).groupId(2L).userId(10L).content("hi").build();

        consumer.consume(e);

        verify(discussionService).appendMessage(e);
        verify(discussionService, never()).summarize(any(), any());
    }

    @Test
    void consume_end_shouldSummarize() {
        DiscussionMessageEvent e = DiscussionMessageEvent.builder()
                .action("END").lessonId(1L).groupId(2L).build();

        consumer.consume(e);

        verify(discussionService).summarize(1L, 2L);
        verify(discussionService, never()).appendMessage(any());
    }

    @Test
    void consume_invalidEvent_shouldNoOp() {
        consumer.consume(DiscussionMessageEvent.builder().action("MESSAGE").lessonId(1L).build()); // groupId null
        verify(discussionService, never()).appendMessage(any());
        verify(discussionService, never()).summarize(any(), any());
    }
}
