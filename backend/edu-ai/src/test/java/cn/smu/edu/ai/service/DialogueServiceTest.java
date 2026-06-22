package cn.smu.edu.ai.service;

import cn.smu.edu.ai.domain.document.AiDialogueMessage;
import cn.smu.edu.ai.domain.document.AiDialogueSession;
import cn.smu.edu.ai.domain.dto.DialogueTaskDTO;
import cn.smu.edu.ai.repository.AiDialogueMessageRepository;
import cn.smu.edu.ai.repository.AiDialogueSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DialogueServiceTest {

    @Mock AiDialogueSessionRepository sessionRepository;
    @Mock AiDialogueMessageRepository messageRepository;

    private DialogueService service() {
        return new DialogueService(sessionRepository, messageRepository);
    }

    private DialogueTaskDTO dto() {
        DialogueTaskDTO d = new DialogueTaskDTO();
        d.setLessonId(1L);
        d.setTopic("函数极限");
        d.setMaxTurns(5);
        return d;
    }

    @Test
    void createSession_shouldSaveActiveSessionAndOpeningMessage() {
        when(messageRepository.countBySessionId(any())).thenReturn(0L);

        AiDialogueSession s = service().createSession(dto(), 9L);

        assertThat(s.getSessionId()).isNotBlank();
        assertThat(s.getStatus()).isEqualTo(AiDialogueSession.STATUS_ACTIVE);
        assertThat(s.getTurnCount()).isZero();
        verify(sessionRepository).save(any(AiDialogueSession.class));
        // 开场白作为 assistant seq=0 入库
        ArgumentCaptor<AiDialogueMessage> cap = ArgumentCaptor.forClass(AiDialogueMessage.class);
        verify(messageRepository).save(cap.capture());
        assertThat(cap.getValue().getRole()).isEqualTo(AiDialogueMessage.ROLE_ASSISTANT);
        assertThat(cap.getValue().getSeq()).isZero();
    }

    @Test
    void saveUserMessage_shouldPersistAndIncrementTurn() {
        AiDialogueSession s = new AiDialogueSession();
        s.setSessionId("sess-1");
        s.setTurnCount(1);
        when(messageRepository.countBySessionId("sess-1")).thenReturn(3L);
        when(sessionRepository.findBySessionId("sess-1")).thenReturn(Optional.of(s));

        AiDialogueMessage m = service().saveUserMessage("sess-1", 9L, "什么是连续？");

        ArgumentCaptor<AiDialogueMessage> cap = ArgumentCaptor.forClass(AiDialogueMessage.class);
        verify(messageRepository).save(cap.capture());
        assertThat(cap.getValue().getRole()).isEqualTo(AiDialogueMessage.ROLE_USER);
        assertThat(cap.getValue().getSeq()).isEqualTo(3);
        // 轮次 +1 后回存
        ArgumentCaptor<AiDialogueSession> sCap = ArgumentCaptor.forClass(AiDialogueSession.class);
        verify(sessionRepository).save(sCap.capture());
        assertThat(sCap.getValue().getTurnCount()).isEqualTo(2);
    }

    @Test
    void saveAssistantMessage_shouldPersistWithAssistantRole() {
        when(messageRepository.countBySessionId("sess-2")).thenReturn(4L);

        service().saveAssistantMessage("sess-2", 9L, "连续是指...");

        ArgumentCaptor<AiDialogueMessage> cap = ArgumentCaptor.forClass(AiDialogueMessage.class);
        verify(messageRepository).save(cap.capture());
        assertThat(cap.getValue().getRole()).isEqualTo(AiDialogueMessage.ROLE_ASSISTANT);
        assertThat(cap.getValue().getSeq()).isEqualTo(4);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void history_shouldReturnOrderedMessages() {
        AiDialogueMessage m = AiDialogueMessage.builder().sessionId("s").role("user").content("hi").seq(0).build();
        when(messageRepository.findBySessionIdOrderBySeqAsc("s")).thenReturn(List.of(m));

        assertThat(service().history("s")).hasSize(1);
    }
}
