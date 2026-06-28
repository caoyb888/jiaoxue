package cn.smu.edu.live.service.impl;

import cn.smu.edu.live.domain.entity.LiveRecord;
import cn.smu.edu.live.repository.LiveRecordMapper;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveStreamServiceImplTest {

    @Mock
    LiveRecordMapper liveRecordMapper;
    @Mock
    MinioClient minioClient;

    LiveStreamServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LiveStreamServiceImpl(liveRecordMapper, minioClient);
        ReflectionTestUtils.setField(service, "replayBucket", "live-replay");
    }

    private LiveRecord record(String streamKey) {
        return LiveRecord.builder().id(5L).lessonId(2L).streamKey(streamKey).status(0).build();
    }

    @Test
    void onPublish_shouldMarkLivingWithStartTime() {
        when(liveRecordMapper.selectByStreamKey("k")).thenReturn(record("k"));

        service.onPublish("k");

        ArgumentCaptor<LiveRecord> captor = ArgumentCaptor.forClass(LiveRecord.class);
        verify(liveRecordMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(captor.getValue().getStartedAt()).isNotNull();
    }

    @Test
    void onUnpublish_shouldMarkEndedAndComputeDuration() {
        LiveRecord r = record("k");
        r.setStartedAt(LocalDateTime.now().minusSeconds(120));
        r.setStatus(1);
        when(liveRecordMapper.selectByStreamKey("k")).thenReturn(r);

        service.onUnpublish("k");

        ArgumentCaptor<LiveRecord> captor = ArgumentCaptor.forClass(LiveRecord.class);
        verify(liveRecordMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(2);
        assertThat(captor.getValue().getEndedAt()).isNotNull();
        assertThat(captor.getValue().getDurationSec()).isBetween(118, 122);
    }

    @Test
    void onDvr_shouldUploadToMinioAndSetReplayPath() throws Exception {
        // UploadObjectArgs.filename() 构建时校验文件存在，故用真实临时 .flv 文件
        java.nio.file.Path dvr = java.nio.file.Files.createTempFile("dvr", ".flv");
        try {
            when(liveRecordMapper.selectByStreamKey("k")).thenReturn(record("k"));
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

            service.onDvr("k", dvr.toString());

            verify(minioClient).uploadObject(any(UploadObjectArgs.class));
            ArgumentCaptor<LiveRecord> captor = ArgumentCaptor.forClass(LiveRecord.class);
            verify(liveRecordMapper).updateById(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(3);
            assertThat(captor.getValue().getReplayPath()).isEqualTo("replay/2/k.flv");
        } finally {
            java.nio.file.Files.deleteIfExists(dvr);
        }
    }

    @Test
    void onDvr_uploadFailure_shouldNotUpdateStatus() throws Exception {
        java.nio.file.Path dvr = java.nio.file.Files.createTempFile("dvr", ".flv");
        try {
            when(liveRecordMapper.selectByStreamKey("k")).thenReturn(record("k"));
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            org.mockito.Mockito.doThrow(new RuntimeException("minio down"))
                    .when(minioClient).uploadObject(any(UploadObjectArgs.class));

            service.onDvr("k", dvr.toString());

            verify(liveRecordMapper, never()).updateById(any(LiveRecord.class));
        } finally {
            java.nio.file.Files.deleteIfExists(dvr);
        }
    }

    @Test
    void callbacks_unknownStreamKey_shouldNoOp() {
        when(liveRecordMapper.selectByStreamKey("ghost")).thenReturn(null);

        service.onPublish("ghost");

        verify(liveRecordMapper, never()).updateById(any(LiveRecord.class));
    }
}
