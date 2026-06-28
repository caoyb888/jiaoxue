package cn.smu.edu.ai.domain.vo;

import cn.smu.edu.ai.domain.document.AiGroupDiscussion;

import java.util.List;

/**
 * 分组讨论 AI 汇总视图（S8-04），供教师端展示。
 */
public record GroupDiscussionVO(
        Long lessonId,
        Long groupId,
        String groupName,
        int participantCount,
        int messageCount,
        String summary,
        List<String> keyPoints,
        String status,
        List<MessageVO> messages) {

    public record MessageVO(Long userId, String userName, String content, String sentAt) {
    }

    public static GroupDiscussionVO of(AiGroupDiscussion doc) {
        List<MessageVO> msgs = doc.getMessages() == null ? List.of()
                : doc.getMessages().stream()
                .map(m -> new MessageVO(m.getUserId(), m.getUserName(), m.getContent(),
                        m.getSentAt() == null ? null : m.getSentAt().toString()))
                .toList();
        return new GroupDiscussionVO(
                doc.getLessonId(),
                doc.getGroupId(),
                doc.getGroupName(),
                doc.getParticipantCount() == null ? 0 : doc.getParticipantCount(),
                msgs.size(),
                doc.getSummary(),
                doc.getKeyPoints() == null ? List.of() : doc.getKeyPoints(),
                doc.getStatus(),
                msgs);
    }
}
