package cn.smu.edu.grade.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 班级成绩规则汇总（含权重合计验证结果） */
@Data
public class GradeRuleListVO {
    private Long classId;
    private List<GradeRuleVO> rules;
    /** 权重合计（满分时应为 100.00） */
    private BigDecimal totalWeight;
    /** 权重是否已配置完成（= 100） */
    private Boolean weightComplete;
}
