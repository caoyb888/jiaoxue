package cn.smu.edu.ai.domain.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 课堂语音转写记录（MongoDB，collection: ai_lecture_transcript）
 *
 * S6-04：科大讯飞 ASR 流式识别，分片增量写入 chunks 数组。
 * 一节课对应一条记录（lessonId 唯一），下课后 status 置 DONE 并回填 fullText。
 * createdAt 作为 S6-15 TTL 索引锚点。
 */
@Data
@Document(collection = "ai_lecture_transcript")
public class AiLectureTranscript {

    @Id
    private String id;

    /** 课堂ID（一节课一条记录） */
    @Indexed(unique = true)
    private Long lessonId;

    private Long teacherId;

    /** 转写状态：RECORDING 进行中 / DONE 已结束 / FAILED 失败 */
    private String status;

    /** 转写分片（按 seq 递增追加） */
    private List<TranscriptChunk> chunks = new ArrayList<>();

    /** 下课后回填的全文（各分片文本拼接） */
    private String fullText;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    /** TTL 索引：课堂转写保留 90 天后由 Mongo 自动删除（S6-15） */
    @Indexed(name = "ttl_created_at", expireAfter = "90d")
    private LocalDateTime createdAt;

    public static final String STATUS_RECORDING = "RECORDING";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_FAILED = "FAILED";
}
