package cn.smu.edu.exam.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuestionBankVO {
    private Long id;
    private Long teacherId;
    private Long deptId;
    private String bankName;
    private String description;
    private Integer isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 是否为当前用户可编辑（creator才能修改/删除） */
    private Boolean editable;
}
