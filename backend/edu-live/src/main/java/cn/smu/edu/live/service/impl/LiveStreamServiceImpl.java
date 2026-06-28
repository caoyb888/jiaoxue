package cn.smu.edu.live.service.impl;

import cn.smu.edu.live.domain.entity.LiveRecord;
import cn.smu.edu.live.repository.LiveRecordMapper;
import cn.smu.edu.live.service.LiveStreamService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * {@link LiveStreamService} 实现。SRS 回调以 streamKey 定位 live_record。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveStreamServiceImpl implements LiveStreamService {

    private static final int STATUS_LIVING = 1;
    private static final int STATUS_ENDED = 2;
    private static final int STATUS_REPLAY_READY = 3;

    @Value("${live.replay-bucket:live-replay}")
    private String replayBucket;

    private final LiveRecordMapper liveRecordMapper;
    private final MinioClient minioClient;

    @Override
    public void onPublish(String streamKey) {
        LiveRecord record = require(streamKey, "on_publish");
        if (record == null) {
            return;
        }
        LiveRecord update = new LiveRecord();
        update.setId(record.getId());
        update.setStatus(STATUS_LIVING);
        update.setStartedAt(LocalDateTime.now());
        liveRecordMapper.updateById(update);
        log.info("SRS on_publish: lessonId={}, streamKey={} 推流中", record.getLessonId(), streamKey);
    }

    @Override
    public void onUnpublish(String streamKey) {
        LiveRecord record = require(streamKey, "on_unpublish");
        if (record == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LiveRecord update = new LiveRecord();
        update.setId(record.getId());
        update.setStatus(STATUS_ENDED);
        update.setEndedAt(now);
        if (record.getStartedAt() != null) {
            update.setDurationSec((int) Duration.between(record.getStartedAt(), now).getSeconds());
        }
        liveRecordMapper.updateById(update);
        log.info("SRS on_unpublish: lessonId={}, streamKey={} 已结束", record.getLessonId(), streamKey);
    }

    @Override
    public void onDvr(String streamKey, String dvrFilePath) {
        LiveRecord record = require(streamKey, "on_dvr");
        if (record == null) {
            return;
        }
        String objectKey = "replay/" + record.getLessonId() + "/" + streamKey + extension(dvrFilePath);
        try {
            ensureBucket();
            minioClient.uploadObject(UploadObjectArgs.builder()
                    .bucket(replayBucket)
                    .object(objectKey)
                    .filename(dvrFilePath)
                    .build());
        } catch (Exception e) {
            // 上传失败不抛出（避免 SRS 反复重试）；保留已结束状态，待人工/重试补传
            log.error("直播回放上传 MinIO 失败: streamKey={}, file={}", streamKey, dvrFilePath, e);
            return;
        }
        LiveRecord update = new LiveRecord();
        update.setId(record.getId());
        update.setStatus(STATUS_REPLAY_READY);
        update.setReplayPath(objectKey);
        liveRecordMapper.updateById(update);
        log.info("SRS on_dvr: lessonId={}, 回放已上传 {}/{}", record.getLessonId(), replayBucket, objectKey);
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(replayBucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(replayBucket).build());
        }
    }

    private LiveRecord require(String streamKey, String action) {
        LiveRecord record = liveRecordMapper.selectByStreamKey(streamKey);
        if (record == null) {
            log.warn("SRS {} 未找到直播记录: streamKey={}", action, streamKey);
        }
        return record;
    }

    /** 取文件扩展名（含点），无则默认 .mp4。 */
    private static String extension(String path) {
        if (path == null) {
            return ".mp4";
        }
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        int dot = path.lastIndexOf('.');
        return dot > slash && dot < path.length() - 1 ? path.substring(dot) : ".mp4";
    }
}
