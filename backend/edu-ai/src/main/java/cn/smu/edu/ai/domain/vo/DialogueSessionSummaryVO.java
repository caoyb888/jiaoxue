package cn.smu.edu.ai.domain.vo;

import cn.smu.edu.ai.domain.document.AiDialogueSession;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师端全班对话概览项（S6-12）。
 */
@Data
public class DialogueSessionSummaryVO {

    private String sessionId;
    private Long userId;
    private String topic;
    private int turnCount;
    private int maxTurns;
    private String status;
    private long messageCount;
    private LocalDateTime updatedAt;

    public static DialogueSessionSummaryVO from(AiDialogueSession s, long messageCount) {
        DialogueSessionSummaryVO vo = new DialogueSessionSummaryVO();
        vo.setSessionId(s.getSessionId());
        vo.setUserId(s.getUserId());
        vo.setTopic(s.getTopic());
        vo.setTurnCount(s.getTurnCount());
        vo.setMaxTurns(s.getMaxTurns());
        vo.setStatus(s.getStatus());
        vo.setMessageCount(messageCount);
        vo.setUpdatedAt(s.getUpdatedAt());
        return vo;
    }
}
