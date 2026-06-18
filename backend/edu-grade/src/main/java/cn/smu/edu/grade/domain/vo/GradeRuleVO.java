package cn.smu.edu.grade.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GradeRuleVO {
    private Long id;
    private Long classId;
    private String ruleName;
    private Integer gradeType;
    private String gradeTypeName;
    private BigDecimal weight;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
