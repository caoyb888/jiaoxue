package cn.smu.edu.exam.domain.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 主观题答案 JSON 结构。
 * 客观题：answerContent 直接存选项字母（"A"/"AB" 等）。
 * 主观题（type=4/5）：answerContent 存此结构的 JSON，
 *   { "text": "答案文字", "attachments": ["exam/files/xxx.jpg"] }
 * 若无附件，前端也可以直接存纯文本字符串（非 JSON）。
 */
@Data
@NoArgsConstructor
public class SubjectiveAnswerContent {

    private String text;
    private List<String> attachments;

    public List<String> getAttachments() {
        return attachments == null ? Collections.emptyList() : attachments;
    }

    /** 是否为含附件的 JSON 格式（以 '{' 开头） */
    public static boolean isJsonFormat(String answerContent) {
        return answerContent != null && answerContent.startsWith("{");
    }
}
