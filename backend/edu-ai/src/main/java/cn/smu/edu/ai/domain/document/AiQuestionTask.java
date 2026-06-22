package cn.smu.edu.ai.domain.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 一键 AI 出题任务（MongoDB，collection: ai_question_task）
 *
 * S6-07：出题参数（主题/题型/数量/难度）随任务落库，供 Consumer 异步出题、
 * 供前端（S6-13）轮询进度。生成完成后写入 questionIds，状态置 DONE。
 */
@Data
@Document(collection = "ai_question_task")
public class AiQuestionTask {

    @Id
    private String id;

    @Indexed(unique = true)
    private String taskId;

    /** 目标题库 → question_bank.id */
    private Long bankId;

    /** 出题教师 → sys_user.id */
    private Long creatorId;

    /** 知识点/主题 */
    private String topic;

    /** 题型列表：1-单选 2-多选 3-判断 4-填空 5-主观 */
    private List<Integer> types;

    /** 期望题目数 */
    private int count;

    /** 难度：1-极易 ~ 5-极难 */
    private int difficulty;

    /** PENDING / GENERATING / DONE / FAILED */
    private String status;

    /** 实际生成入库的题目数 */
    private int generatedCount;

    /** 入库题目ID列表 */
    private List<Long> questionIds;

    private String errorMsg;

    @Indexed
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_GENERATING = "GENERATING";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_FAILED = "FAILED";
}
