package cn.smu.edu.ai.service.impl;

import cn.smu.edu.ai.domain.document.AiGroupDiscussion;
import cn.smu.edu.ai.domain.document.AiGroupDiscussion.DiscussionMessage;
import cn.smu.edu.ai.domain.model.AiRequest;
import cn.smu.edu.ai.domain.model.ModelType;
import cn.smu.edu.ai.repository.AiGroupDiscussionRepository;
import cn.smu.edu.ai.service.AiGatewayService;
import cn.smu.edu.ai.service.DiscussionService;
import cn.smu.edu.common.event.DiscussionMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link DiscussionService} 实现。LLM 汇总统一走 {@link AiGatewayService}（C4 安全层）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscussionServiceImpl implements DiscussionService {

    static final String STATUS_COLLECTING = "COLLECTING";
    static final String STATUS_SUMMARIZED = "SUMMARIZED";

    private static final String SYSTEM_PROMPT = """
            你是课堂分组讨论分析助手。请仅基于以下学生讨论记录，输出两部分：
            第一部分：讨论主题概要与参与活跃度简评；
            第二部分：关键观点，每条以"- "开头另起一行。
            不得编造记录中未出现的内容。
            """;

    private final AiGroupDiscussionRepository repository;
    private final AiGatewayService aiGatewayService;

    @Override
    public void appendMessage(DiscussionMessageEvent event) {
        if (event.getLessonId() == null || event.getGroupId() == null
                || !StringUtils.hasText(event.getContent())) {
            return;
        }
        AiGroupDiscussion doc = repository.findByLessonIdAndGroupId(event.getLessonId(), event.getGroupId())
                .orElseGet(() -> AiGroupDiscussion.builder()
                        .lessonId(event.getLessonId())
                        .groupId(event.getGroupId())
                        .groupName(event.getGroupName())
                        .messages(new ArrayList<>())
                        .keyPoints(new ArrayList<>())
                        .status(STATUS_COLLECTING)
                        .createdAt(LocalDateTime.now())
                        .build());

        if (StringUtils.hasText(event.getGroupName())) {
            doc.setGroupName(event.getGroupName());
        }
        doc.getMessages().add(DiscussionMessage.builder()
                .userId(event.getUserId())
                .userName(event.getUserName())
                .content(event.getContent())
                .sentAt(event.getSentAt() != null ? event.getSentAt() : LocalDateTime.now())
                .build());
        doc.setParticipantCount((int) doc.getMessages().stream()
                .map(DiscussionMessage::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct().count());
        doc.setUpdatedAt(LocalDateTime.now());
        repository.save(doc);
    }

    @Override
    public AiGroupDiscussion summarize(Long lessonId, Long groupId) {
        AiGroupDiscussion doc = repository.findByLessonIdAndGroupId(lessonId, groupId).orElse(null);
        if (doc == null || doc.getMessages().isEmpty()) {
            log.info("分组讨论无消息，跳过汇总: lessonId={}, groupId={}", lessonId, groupId);
            return doc;
        }
        String transcript = doc.getMessages().stream()
                .map(m -> "学生" + (m.getUserName() == null ? m.getUserId() : m.getUserName()) + "：" + m.getContent())
                .collect(Collectors.joining("\n"));

        AiRequest req = AiRequest.builder()
                .systemPrompt(SYSTEM_PROMPT)
                .userPrompt(transcript)
                .modelType(ModelType.ANALYSIS)
                .lessonId(lessonId)
                .build();
        String raw = aiGatewayService.chatSync(req); // C4：网关内部过 PromptSecurityFilter

        doc.setSummary(raw);
        doc.setKeyPoints(parseKeyPoints(raw));
        doc.setStatus(STATUS_SUMMARIZED);
        doc.setUpdatedAt(LocalDateTime.now());
        repository.save(doc);
        log.info("分组讨论 AI 汇总完成: lessonId={}, groupId={}, 消息数={}", lessonId, groupId, doc.getMessages().size());
        return doc;
    }

    @Override
    public AiGroupDiscussion getDiscussion(Long lessonId, Long groupId) {
        return repository.findByLessonIdAndGroupId(lessonId, groupId).orElse(null);
    }

    /** 从 LLM 输出提取以 - / • / * 开头的关键观点行。 */
    private static List<String> parseKeyPoints(String raw) {
        List<String> points = new ArrayList<>();
        if (raw == null) {
            return points;
        }
        for (String line : raw.split("\\r?\\n")) {
            String t = line.strip();
            if (t.startsWith("- ") || t.startsWith("• ") || t.startsWith("* ")) {
                points.add(t.substring(2).strip());
            }
        }
        return points;
    }
}
