package cn.smu.edu.exam.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** docx 批量导题结果 */
@Data
@AllArgsConstructor
public class DocxImportResultVO {
    /** 识别到的题目总数 */
    private Integer total;
    /** 成功导入数 */
    private Integer successCount;
    /** 失败数（含格式错误、缺少答案等） */
    private Integer failCount;
    /** 各题明细 */
    private List<QuestionImportItemVO> details;
}
