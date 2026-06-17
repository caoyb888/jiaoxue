package cn.smu.edu.exam.domain.dto;

import lombok.Data;

@Data
public class QuestionBankQueryDTO {

    private String keyword;

    /** null-全部 0-私有 1-院系共享 */
    private Integer isPublic;

    private int page = 1;
    private int size = 20;
}
