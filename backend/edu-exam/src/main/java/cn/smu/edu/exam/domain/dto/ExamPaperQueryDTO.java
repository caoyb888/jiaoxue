package cn.smu.edu.exam.domain.dto;

import lombok.Data;

@Data
public class ExamPaperQueryDTO {

    private String keyword;

    /** null-全部 0-固定 1-随机 */
    private Integer isRandom;

    private int page = 1;
    private int size = 20;
}
