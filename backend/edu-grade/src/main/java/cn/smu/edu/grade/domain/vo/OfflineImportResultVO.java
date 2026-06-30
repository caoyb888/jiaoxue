package cn.smu.edu.grade.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** 线下成绩 xlsx 导入结果。 */
@Data
@AllArgsConstructor
public class OfflineImportResultVO {
    /** 解析到的数据行总数（不含表头）。 */
    private Integer total;
    /** 成功写入数。 */
    private Integer successCount;
    /** 失败数（学号缺失、成绩非法等）。 */
    private Integer failCount;
    /** 失败行明细（行号 + 原因），供前端展示。 */
    private List<String> errors;
}
