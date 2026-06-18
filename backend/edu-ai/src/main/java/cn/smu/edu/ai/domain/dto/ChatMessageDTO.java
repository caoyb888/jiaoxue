package cn.smu.edu.ai.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageDTO {

    @NotBlank
    @Size(max = 2000)
    private String content;
}
