package cn.smu.edu.exam.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生视角的考试发布信息：
 * - 隐藏 passwordHash（用 hasPassword 代替）
 * - 若 answerShowAt 未到，题目中的 answer/analysis/isCorrect 被清空
 */
@Data
public class ExamPublishStudentVO {
    private Long id;
    private Long paperId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMin;
    private Boolean hasPassword;
    private Integer enableMonitor;
    private Integer faceVerifyType;
    private Integer allowCopy;
    private Integer shuffleQuestion;
    private Integer shuffleOption;
    private Integer status;
    private String statusLabel;
    /** 答案是否当前可见 */
    private Boolean answerVisible;
    /** 题目列表（考试进行中时包含，answerVisible=false时隐藏答案字段） */
    private List<PaperQuestionVO> questions;
}
