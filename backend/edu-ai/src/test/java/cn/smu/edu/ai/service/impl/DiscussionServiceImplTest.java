package cn.smu.edu.ai.service.impl;

import cn.smu.edu.ai.domain.document.AiGroupDiscussion;
import cn.smu.edu.ai.domain.model.AiRequest;
import cn.smu.edu.ai.repository.AiGroupDiscussionRepository;
import cn.smu.edu.ai.service.AiGatewayService;
import cn.smu.edu.common.event.DiscussionMessageEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscussionServiceImplTest {

    @Mock
    AiGroupDiscussionRepository repository;
    @Mock
    AiGatewayService aiGatewayService;

    private DiscussionServiceImpl service() {
        return new DiscussionServiceImpl(repository, aiGatewayService);
    }

    private DiscussionMessageEvent msg(Long userId, String content) {
        return DiscussionMessageEvent.builder()
                .action("MESSAGE").lessonId(1L).groupId(2L).groupName("第1组")
                .userId(userId).userName("U" + userId).content(content)
                .sentAt(LocalDateTime.now()).build();
    }

    @Test
    void appendMessage_shouldCreateDocAndCountDistinctParticipants() {
        when(repository.findByLessonIdAndGroupId(1L, 2L)).thenReturn(Optional.empty());

        service().appendMessage(msg(10L, "我觉得方案A好"));

        ArgumentCaptor<AiGroupDiscussion> captor = ArgumentCaptor.forClass(AiGroupDiscussion.class);
        verify(repository).save(captor.capture());
        AiGroupDiscussion doc = captor.getValue();
        assertThat(doc.getMessages()).hasSize(1);
        assertThat(doc.getParticipantCount()).isEqualTo(1);
        assertThat(doc.getStatus()).isEqualTo("COLLECTING");
        assertThat(doc.getGroupName()).isEqualTo("第1组");
    }

    @Test
    void appendMessage_existingDoc_shouldAppendAndDedupParticipants() {
        AiGroupDiscussion existing = AiGroupDiscussion.builder()
                .lessonId(1L).groupId(2L).messages(new ArrayList<>())
                .keyPoints(new ArrayList<>()).status("COLLECTING").build();
        existing.getMessages().add(AiGroupDiscussion.DiscussionMessage.builder()
                .userId(10L).userName("U10").content("先发言").sentAt(LocalDateTime.now()).build());
        when(repository.findByLessonIdAndGroupId(1L, 2L)).thenReturn(Optional.of(existing));

        service().appendMessage(msg(10L, "补充一点")); // 同一用户再发

        ArgumentCaptor<AiGroupDiscussion> captor = ArgumentCaptor.forClass(AiGroupDiscussion.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getMessages()).hasSize(2);
        assertThat(captor.getValue().getParticipantCount()).isEqualTo(1); // 去重仍 1 人
    }

    @Test
    void appendMessage_blankContent_shouldBeIgnored() {
        service().appendMessage(DiscussionMessageEvent.builder()
                .lessonId(1L).groupId(2L).userId(1L).content("  ").build());
        verify(repository, never()).save(any());
    }

    @Test
    void summarize_shouldCallGatewayAndParseKeyPoints() {
        AiGroupDiscussion doc = AiGroupDiscussion.builder()
                .lessonId(1L).groupId(2L).messages(new ArrayList<>())
                .keyPoints(new ArrayList<>()).status("COLLECTING").build();
        doc.getMessages().add(AiGroupDiscussion.DiscussionMessage.builder()
                .userId(10L).userName("张三").content("方案A可行").sentAt(LocalDateTime.now()).build());
        when(repository.findByLessonIdAndGroupId(1L, 2L)).thenReturn(Optional.of(doc));
        when(aiGatewayService.chatSync(any(AiRequest.class)))
                .thenReturn("讨论概要：聚焦方案A。\n- 方案A可行\n- 需评估成本");

        AiGroupDiscussion result = service().summarize(1L, 2L);

        // 校验送入 LLM 的 prompt 含讨论记录
        ArgumentCaptor<AiRequest> reqCaptor = ArgumentCaptor.forClass(AiRequest.class);
        verify(aiGatewayService).chatSync(reqCaptor.capture());
        assertThat(reqCaptor.getValue().getUserPrompt()).contains("张三").contains("方案A可行");

        assertThat(result.getStatus()).isEqualTo("SUMMARIZED");
        assertThat(result.getSummary()).contains("聚焦方案A");
        assertThat(result.getKeyPoints()).containsExactly("方案A可行", "需评估成本");
        verify(repository).save(doc);
    }

    @Test
    void summarize_noMessages_shouldSkipLlm() {
        when(repository.findByLessonIdAndGroupId(1L, 2L)).thenReturn(Optional.empty());

        AiGroupDiscussion result = service().summarize(1L, 2L);

        assertThat(result).isNull();
        verify(aiGatewayService, never()).chatSync(any());
    }
}
