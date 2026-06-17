package cn.smu.edu.exam.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 单道题目导入结果明细 */
@Data
@AllArgsConstructor
public class QuestionImportItemVO {
    /** 题目在文档中的序号（从1开始） */
    private Integer index;
    /** 题目内容前 50 字（用于定位） */
    private String contentPreview;
    /** 是否成功 */
    private Boolean success;
    /** 失败时的错误描述 */
    private String errorMessage;

    public static QuestionImportItemVO success(int index, String content) {
        return new QuestionImportItemVO(index, preview(content), true, null);
    }

    public static QuestionImportItemVO fail(int index, String content, String errorMessage) {
        return new QuestionImportItemVO(index, preview(content), false, errorMessage);
    }

    private static String preview(String content) {
        if (content == null) return "";
        return content.length() > 50 ? content.substring(0, 50) + "…" : content;
    }
}
