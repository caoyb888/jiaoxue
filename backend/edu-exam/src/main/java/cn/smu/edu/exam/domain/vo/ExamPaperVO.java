package cn.smu.edu.exam.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExamPaperVO {
    private Long id;
    private Long creatorId;
    private String title;
    private BigDecimal totalScore;
    private Integer isRandom;
    private String paperType;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 是否可编辑（当前用户是creator） */
    private Boolean editable;
}
