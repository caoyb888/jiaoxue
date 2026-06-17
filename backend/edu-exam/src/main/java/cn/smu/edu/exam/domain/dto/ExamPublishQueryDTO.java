package cn.smu.edu.exam.domain.dto;

import lombok.Data;

@Data
public class ExamPublishQueryDTO {

    private Long classId;

    /** null=全部 0-未开始 1-进行中 2-已结束 3-已取消 */
    private Integer status;

    private int page = 1;
    private int size = 20;
}
