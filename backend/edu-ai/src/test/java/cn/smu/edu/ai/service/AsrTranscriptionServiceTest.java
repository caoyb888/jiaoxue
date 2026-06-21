package cn.smu.edu.ai.service;

import cn.smu.edu.ai.asr.AsrClient;
import cn.smu.edu.ai.asr.MockAsrClient;
import cn.smu.edu.ai.domain.document.AiLectureTranscript;
import cn.smu.edu.ai.domain.document.TranscriptChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AsrTranscriptionServiceTest {

    @Mock MongoTemplate mongoTemplate;
    @Mock AiNotifyPublisher notifyPublisher;

    /** 用真实 MockAsrClient（每帧同步回调一个落定句段），便于断言链路 */
    private final AsrClient asrClient = new MockAsrClient();

    private AsrTranscriptionService service() {
        return new AsrTranscriptionService(asrClient, mongoTemplate, notifyPublisher);
    }

    @Test
    void start_shouldUpsertRecordingRecord() {
        service().start(1L, 9L, null);

        verify(mongoTemplate).upsert(any(), any(Update.class), eq(AiLectureTranscript.class));
    }

    @Test
    void onAudio_shouldPushChunkAndForwardToSinkAndBroadcast() {
        AsrTranscriptionService svc = service();
        List<TranscriptChunk> received = new ArrayList<>();
        svc.start(2L, 9L, received::add);

        svc.onAudio(2L, new byte[]{1, 2, 3, 4});

        // 增量 $push 一个分片
        verify(mongoTemplate, atLeastOnce()).updateFirst(any(), any(Update.class), eq(AiLectureTranscript.class));
        // 实时字幕回传 sink
        assertThat(received).hasSize(1);
        assertThat(received.get(0).getSeq()).isZero();
        assertThat(received.get(0).getText()).contains("模拟转写");
        assertThat(received.get(0).isFinalFlag()).isTrue();
        // 课堂广播
        verify(notifyPublisher).notifyLesson(eq(2L), eq("ASR_CHUNK"), anyString(), any());
    }

    @Test
    void onAudio_shouldIncrementSeqAcrossFrames() {
        AsrTranscriptionService svc = service();
        List<TranscriptChunk> received = new ArrayList<>();
        svc.start(3L, 9L, received::add);

        svc.onAudio(3L, new byte[]{1});
        svc.onAudio(3L, new byte[]{2});
        svc.onAudio(3L, new byte[]{3});

        assertThat(received).extracting(TranscriptChunk::getSeq).containsExactly(0, 1, 2);
    }

    @Test
    void onAudio_shouldIgnore_whenNoActiveSession() {
        service().onAudio(404L, new byte[]{1, 2});

        verify(notifyPublisher, org.mockito.Mockito.never()).notifyLesson(any(), any(), any(), any());
    }

    @Test
    void stop_shouldFinalizeWithDoneStatusAndFullText() {
        AsrTranscriptionService svc = service();
        svc.start(4L, 9L, null);
        svc.onAudio(4L, new byte[]{1});
        svc.onAudio(4L, new byte[]{2});

        svc.stop(4L);

        // 最后一次 updateFirst 为收尾：status=DONE + fullText + endedAt
        ArgumentCaptor<Update> cap = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate, atLeastOnce()).updateFirst(any(), cap.capture(), eq(AiLectureTranscript.class));
        Update finalize = cap.getAllValues().get(cap.getAllValues().size() - 1);
        org.bson.Document set = finalize.getUpdateObject().get("$set", org.bson.Document.class);
        assertThat(set).containsKeys("status", "fullText", "endedAt");
        assertThat(set.getString("status")).isEqualTo(AiLectureTranscript.STATUS_DONE);
    }

    @Test
    void start_shouldIgnoreDuplicateSession() {
        AsrTranscriptionService svc = service();
        svc.start(5L, 9L, null);
        svc.start(5L, 9L, null); // 重复开启应被忽略

        // upsert 只发生一次
        verify(mongoTemplate, org.mockito.Mockito.times(1))
                .upsert(any(), any(Update.class), eq(AiLectureTranscript.class));
    }
}
