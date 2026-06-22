package cn.smu.edu.ai.domain.vo;

import cn.smu.edu.ai.domain.document.AiDialogueMessage;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话历史消息项。
 */
@Data
public class DialogueMessageVO {

    private String role;
    private String content;
    private int seq;
    private LocalDateTime createdAt;

    public static DialogueMessageVO from(AiDialogueMessage m) {
        DialogueMessageVO vo = new DialogueMessageVO();
        vo.setRole(m.getRole());
        vo.setContent(m.getContent());
        vo.setSeq(m.getSeq());
        vo.setCreatedAt(m.getCreatedAt());
        return vo;
    }
}
