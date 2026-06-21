package cn.smu.edu.ai.service;

import cn.smu.edu.ai.asr.AsrClient;
import cn.smu.edu.ai.asr.AsrSegment;
import cn.smu.edu.ai.asr.AsrSession;
import cn.smu.edu.ai.domain.document.AiLectureTranscript;
import cn.smu.edu.ai.domain.document.TranscriptChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * S6-04 课堂语音转写服务。
 *
 * 一节课一个流式会话：start 初始化 ai_lecture_transcript 记录并打开 ASR 上游会话；
 * 每帧音频经 ASR 识别为落定句段，增量 $push 进 chunks 数组（避免整文档读改写），
 * 同时回传给客户端（实时字幕 sink）并广播到课堂（WebSocket）；stop 时回填 fullText、置 DONE。
 *
 * 高并发说明：与签到/交卷类似，转写采用增量 $push 单次更新，不在循环中整体回写。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsrTranscriptionService {

    private final AsrClient asrClient;
    private final MongoTemplate mongoTemplate;
    private final AiNotifyPublisher notifyPublisher;

    /** lessonId -> 会话状态 */
    private final Map<Long, Session> sessions = new ConcurrentHashMap<>();

    /** 单节课转写会话的运行态 */
    private static final class Session {
        final Long lessonId;
        final Long teacherId;
        final AsrSession asr;
        final AtomicInteger seq = new AtomicInteger(0);
        final StringBuilder fullText = new StringBuilder();

        Session(Long lessonId, Long teacherId, AsrSession asr) {
            this.lessonId = lessonId;
            this.teacherId = teacherId;
            this.asr = asr;
        }
    }

    /**
     * 开启转写会话。
     *
     * @param sink 落定分片实时回传（如经 WebSocket 推给教师端做字幕）；可为 null
     */
    public void start(Long lessonId, Long teacherId, Consumer<TranscriptChunk> sink) {
        if (sessions.containsKey(lessonId)) {
            log.warn("课堂转写会话已存在，忽略重复开启: lessonId={}", lessonId);
            return;
        }
        upsertRecording(lessonId, teacherId);

        AsrSession asr = asrClient.open("asr-" + lessonId,
                segment -> onSegment(lessonId, segment, sink));
        sessions.put(lessonId, new Session(lessonId, teacherId, asr));
        log.info("课堂转写会话开启: lessonId={}, teacherId={}", lessonId, teacherId);
    }

    /** 投递一帧课堂音频 */
    public void onAudio(Long lessonId, byte[] frame) {
        Session s = sessions.get(lessonId);
        if (s == null) {
            log.warn("收到音频但无活跃转写会话: lessonId={}", lessonId);
            return;
        }
        s.asr.sendAudio(frame);
    }

    /** 结束转写会话：触发尾包、回填全文、置 DONE */
    public void stop(Long lessonId) {
        Session s = sessions.remove(lessonId);
        if (s == null) {
            return;
        }
        try {
            s.asr.endStream();
        } catch (Exception e) {
            log.warn("ASR 尾包发送失败: lessonId={}", lessonId, e);
        } finally {
            try {
                s.asr.close();
            } catch (Exception ignore) {
                // 关闭异常不影响落库
            }
        }
        mongoTemplate.updateFirst(
                lessonQuery(lessonId),
                new Update()
                        .set("status", AiLectureTranscript.STATUS_DONE)
                        .set("fullText", s.fullText.toString())
                        .set("endedAt", LocalDateTime.now()),
                AiLectureTranscript.class);
        log.info("课堂转写会话结束: lessonId={}, 分片数={}", lessonId, s.seq.get());
    }

    private void onSegment(Long lessonId, AsrSegment segment, Consumer<TranscriptChunk> sink) {
        Session s = sessions.get(lessonId);
        if (s == null || segment == null || segment.text() == null || segment.text().isBlank()) {
            return;
        }
        TranscriptChunk chunk = TranscriptChunk.builder()
                .seq(s.seq.getAndIncrement())
                .text(segment.text())
                .beginMs(segment.beginMs())
                .endMs(segment.endMs())
                .finalFlag(segment.finalFlag())
                .createdAt(LocalDateTime.now())
                .build();

        // 增量追加分片，避免整文档读改写
        mongoTemplate.updateFirst(
                lessonQuery(lessonId),
                new Update().push("chunks", chunk),
                AiLectureTranscript.class);
        s.fullText.append(segment.text());

        if (sink != null) {
            sink.accept(chunk);
        }
        // 课堂广播实时字幕（经 edu.notice → edu-notify → STOMP）
        notifyPublisher.notifyLesson(lessonId, "ASR_CHUNK", segment.text(),
                Map.of("seq", chunk.getSeq(), "beginMs", chunk.getBeginMs(), "endMs", chunk.getEndMs()));
    }

    /** 初始化/重置一条 RECORDING 记录（lessonId 唯一，重开覆盖） */
    private void upsertRecording(Long lessonId, Long teacherId) {
        LocalDateTime now = LocalDateTime.now();
        mongoTemplate.upsert(
                lessonQuery(lessonId),
                new Update()
                        .set("teacherId", teacherId)
                        .set("status", AiLectureTranscript.STATUS_RECORDING)
                        .set("chunks", java.util.List.of())
                        .set("fullText", null)
                        .set("startedAt", now)
                        .set("endedAt", null)
                        .setOnInsert("createdAt", now),
                AiLectureTranscript.class);
    }

    private Query lessonQuery(Long lessonId) {
        return new Query(Criteria.where("lessonId").is(lessonId));
    }
}
