package cn.smu.edu.exam.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 课堂当前题目（供学生端展示） */
@Data
public class LessonQuestionVO {
    private Long id;         // lesson_question.id
    private Long lessonId;
    private Long questionId;
    private Integer questionType;  // 1-单选 2-多选 3-判断 4-填空 5-主观 6-投票
    private String content;
    /** 0-进行中 1-已关闭 */
    private Integer status;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    /** 选项（仅选择题/投票题） */
    private List<QuestionOptionVO> options;
}
