package cn.smu.edu.exam.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 课堂随堂答题记录（学生对教师推送的 lesson_question 的作答） */
@Data
@TableName("lesson_question_answer")
public class LessonQuestionAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lessonQuestionId;
    private Long lessonId;
    private Long questionId;
    private Long studentId;

    private String answerContent;

    /** NULL=未判定（填空/主观/投票） 0=错 1=对 */
    private Integer isCorrect;

    private LocalDateTime submittedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
