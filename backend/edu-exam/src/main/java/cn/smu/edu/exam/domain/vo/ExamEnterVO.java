package cn.smu.edu.exam.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 进入考试响应：包含考试配置信息、首页题目（分批下题）及监考会话状态。
 * questions 仅含第一批题目（page=1），后续用 GET /questions?page=N 拉取。
 */
@Data
public class ExamEnterVO {
    private Long publishId;
    private Long paperId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMin;
    private Integer enableMonitor;
    private Integer faceVerifyType;
    private Integer allowCopy;
    private Integer shuffleQuestion;
    private Integer shuffleOption;

    /** 当前监考会话状态：VERIFYING / ANSWERING */
    private String sessionStatus;
    /** 人脸核验是否已通过（null=未核验，0=未通过，1=通过） */
    private Integer faceVerifyPassed;
    /** 是否首次进入（false=断线重连，可从 IndexedDB 恢复草稿） */
    private Boolean firstEnter;

    /** 当前页题目 */
    private List<PaperQuestionVO> questions;
    /** 题目总数 */
    private Integer totalQuestions;
    /** 总页数（每页10题） */
    private Integer totalPages;
    /** 当前页码 */
    private Integer currentPage;
}
