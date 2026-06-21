package cn.smu.edu.exam.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 随堂答题作答结果（客观题即时判对错，主观/填空待教师批改） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonAnswerResultVO {

    private Long lessonQuestionId;
    /** 1-单选 2-多选 3-判断 4-填空 5-主观 6-投票 */
    private Integer questionType;
    /** null=主观/填空/投票（不判对错） true/false=客观题判定结果 */
    private Boolean isCorrect;
    /** 仅客观题返回标准答案 */
    private String correctAnswer;
    private String analysis;
    private String myAnswer;
}
