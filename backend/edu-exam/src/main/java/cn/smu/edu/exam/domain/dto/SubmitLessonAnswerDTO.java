package cn.smu.edu.exam.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 学生随堂答题提交请求 */
@Data
public class SubmitLessonAnswerDTO {

    @NotNull(message = "题目ID不能为空")
    private Long lessonQuestionId;

    /** 作答内容：单选"A"、多选"A,C"、判断"true"/"false"、填空/主观为文本 */
    @NotBlank(message = "作答内容不能为空")
    private String answer;
}
