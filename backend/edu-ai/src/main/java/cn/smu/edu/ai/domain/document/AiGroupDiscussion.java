package cn.smu.edu.ai.domain.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 分组讨论 AI 汇总（MongoDB，collection: ai_group_discussion，S8-04）。
 *
 * <p>DiscussionConsumer 收集学生讨论发言（messages），讨论结束触发 LLM 分析，
 * 写入 summary / keyPoints。一节课每组一条（lessonId+groupId 唯一）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "uk_lesson_group", def = "{'lessonId': 1, 'groupId': 1}", unique = true)
@Document(collection = "ai_group_discussion")
public class AiGroupDiscussion {

    @Id
    private String id;

    private Long lessonId;
    private Long groupId;
    private String groupName;

    /** 讨论发言流水。 */
    @Builder.Default
    private List<DiscussionMessage> messages = new ArrayList<>();

    /** 去重参与人数。 */
    private Integer participantCount;

    /** LLM 汇总（讨论主题概要 + 活跃度简评）。 */
    private String summary;

    /** LLM 提取的关键观点。 */
    @Builder.Default
    private List<String> keyPoints = new ArrayList<>();

    /** 状态：COLLECTING-收集中 / SUMMARIZED-已汇总。 */
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscussionMessage {
        private Long userId;
        private String userName;
        private String content;
        private LocalDateTime sentAt;
    }
}
