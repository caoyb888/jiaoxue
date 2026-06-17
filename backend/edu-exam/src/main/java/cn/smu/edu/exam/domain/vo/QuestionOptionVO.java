package cn.smu.edu.exam.domain.vo;

import lombok.Data;

@Data
public class QuestionOptionVO {
    private Long id;
    private String optionLabel;
    private String content;
    private Integer isCorrect;
    private Integer sortOrder;
}
