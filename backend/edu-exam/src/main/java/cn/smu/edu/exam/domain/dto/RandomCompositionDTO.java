package cn.smu.edu.exam.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** 随机组卷请求：先清空当前卷组，再按规则抽题重组 */
@Data
public class RandomCompositionDTO {

    /** 要重置的卷组（A/B/C）；null=重置全部 */
    private String clearGroup;

    @NotEmpty(message = "抽题规则不能为空")
    @Valid
    private List<RandomPickRuleDTO> rules;
}
