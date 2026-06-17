package cn.smu.edu.exam.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchAddQuestionsDTO {

    @NotEmpty(message = "题目列表不能为空")
    @Valid
    private List<AddQuestionDTO> questions;
}
