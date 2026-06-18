package cn.smu.edu.exam.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 课堂发题请求 */
@Data
public class PublishLessonQuestionDTO {

    @NotNull
    private Long questionId;
}
