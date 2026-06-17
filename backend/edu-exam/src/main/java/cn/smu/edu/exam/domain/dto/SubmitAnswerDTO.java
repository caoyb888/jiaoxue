package cn.smu.edu.exam.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** 简化版交卷（完整版含 IndexedDB 草稿打散在 S5 实现） */
@Data
public class SubmitAnswerDTO {

    @NotEmpty(message = "答案列表不能为空")
    @Valid
    private List<AnswerItemDTO> answers;
}
