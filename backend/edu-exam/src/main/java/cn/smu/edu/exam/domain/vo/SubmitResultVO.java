package cn.smu.edu.exam.domain.vo;

import lombok.Data;

import java.util.List;

/** 交卷响应：含各题批改结果（客观题立即得到结果，主观题 reviewStatus=0） */
@Data
public class SubmitResultVO {
    private Long publishId;
    private Long studentId;
    private Integer submittedCount;
    private List<GradeResultVO> gradeResults;
}
