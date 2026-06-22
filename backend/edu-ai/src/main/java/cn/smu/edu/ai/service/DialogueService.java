package cn.smu.edu.ai.service;

import cn.smu.edu.ai.domain.document.AiDialogueMessage;
import cn.smu.edu.ai.domain.document.AiDialogueSession;
import cn.smu.edu.ai.domain.dto.DialogueTaskDTO;
import cn.smu.edu.ai.domain.model.ModelType;
import cn.smu.edu.ai.repository.AiDialogueMessageRepository;
import cn.smu.edu.ai.repository.AiDialogueSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * S6-08 AI 对话会话/消息持久化服务。
 *
 * 流式回复本身在 {@link AiGatewayService#chat} + Controller SSE 中完成；
 * 本服务负责会话创建、消息落库（user/assistant）、轮次控制与历史查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DialogueService {

    private static final String DEFAULT_OPENING = "请就本课主题与我展开讨论...";

    private final AiDialogueSessionRepository sessionRepository;
    private final AiDialogueMessageRepository messageRepository;

    /** 创建对话会话，开场白作为 assistant 首条消息落库 */
    public AiDialogueSession createSession(DialogueTaskDTO dto, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        AiDialogueSession session = new AiDialogueSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setLessonId(dto.getLessonId());
        session.setTopic(dto.getTopic());
        session.setOpening(dto.getOpening() != null && !dto.getOpening().isBlank()
                ? dto.getOpening() : DEFAULT_OPENING);
        session.setMaxTurns(dto.getMaxTurns());
        session.setTurnCount(0);
        session.setModelType(dto.getModelType() != null ? dto.getModelType().name() : ModelType.ANALYSIS.name());
        session.setStatus(AiDialogueSession.STATUS_ACTIVE);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        sessionRepository.save(session);

        // 开场白入库（assistant，seq=0）
        saveMessage(session.getSessionId(), userId, AiDialogueMessage.ROLE_ASSISTANT, session.getOpening());
        log.info("AI对话会话已创建: sessionId={}, userId={}, topic={}",
                session.getSessionId(), userId, dto.getTopic());
        return session;
    }

    public AiDialogueSession getSession(String sessionId) {
        return sessionRepository.findBySessionId(sessionId).orElse(null);
    }

    /** 保存学生发言并使会话轮次 +1 */
    public AiDialogueMessage saveUserMessage(String sessionId, Long userId, String content) {
        AiDialogueMessage msg = saveMessage(sessionId, userId, AiDialogueMessage.ROLE_USER, content);
        sessionRepository.findBySessionId(sessionId).ifPresent(s -> {
            s.setTurnCount(s.getTurnCount() + 1);
            s.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(s);
        });
        return msg;
    }

    /** 保存 AI 回复（流式聚合后的全文） */
    public AiDialogueMessage saveAssistantMessage(String sessionId, Long userId, String content) {
        return saveMessage(sessionId, userId, AiDialogueMessage.ROLE_ASSISTANT, content);
    }

    public List<AiDialogueMessage> history(String sessionId) {
        return messageRepository.findBySessionIdOrderBySeqAsc(sessionId);
    }

    /** 某节课全部对话会话（教师端概览，按最近活跃排序） */
    public List<AiDialogueSession> listByLesson(Long lessonId) {
        return sessionRepository.findByLessonIdOrderByUpdatedAtDesc(lessonId);
    }

    public long messageCount(String sessionId) {
        return messageRepository.countBySessionId(sessionId);
    }

    /** 将消息标记为安全拦截（C4：is_filtered=true） */
    public void markFiltered(String messageId) {
        if (messageId == null) {
            return;
        }
        messageRepository.findById(messageId).ifPresent(m -> {
            m.setFiltered(true);
            messageRepository.save(m);
            log.warn("对话消息被安全拦截标记: messageId={}, sessionId={}", messageId, m.getSessionId());
        });
    }

    private AiDialogueMessage saveMessage(String sessionId, Long userId, String role, String content) {
        int seq = (int) messageRepository.countBySessionId(sessionId);
        AiDialogueMessage msg = AiDialogueMessage.builder()
                .sessionId(sessionId)
                .userId(userId)
                .role(role)
                .content(content)
                .seq(seq)
                .createdAt(LocalDateTime.now())
                .build();
        return messageRepository.save(msg);
    }
}
