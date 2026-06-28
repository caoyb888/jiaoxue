package cn.smu.edu.live.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.live.domain.dto.LessonLiveInfo;
import cn.smu.edu.live.domain.entity.LiveRecord;
import cn.smu.edu.live.domain.vo.LiveConfigVO;
import cn.smu.edu.live.repository.LessonLiveMapper;
import cn.smu.edu.live.repository.LiveRecordMapper;
import cn.smu.edu.live.service.LiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@link LiveService} 实现。落实 C5：SLIDE_ONLY 不开任何流媒体。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveServiceImpl implements LiveService {

    static final String MODE_SLIDE_ONLY = "SLIDE_ONLY";
    static final String MODE_ONLINE_CLASS = "ONLINE_CLASS";

    private static final int STATUS_PENDING = 0;   // 待推流
    private static final int STATUS_ENDED = 2;     // 已结束（1-推流中由 SRS 回调置位，S8-02）

    /** RTMP 推流基地址，如 rtmp://host:1935/live。 */
    @Value("${live.rtmp-base:rtmp://100.84.68.115:1935/live}")
    private String rtmpBase;

    /** HLS 拉流基地址，如 http://host:8080/live。 */
    @Value("${live.hls-base:http://100.84.68.115:8080/live}")
    private String hlsBase;

    private final LiveRecordMapper liveRecordMapper;
    private final LessonLiveMapper lessonLiveMapper;

    @Override
    public LiveConfigVO startLive(Long lessonId, Long operatorId) {
        LessonLiveInfo lesson = requireLesson(lessonId);

        // C5：线下课堂仅 WebSocket，不创建直播记录、不下发任何流媒体地址
        if (!MODE_ONLINE_CLASS.equals(lesson.getLiveMode())) {
            log.info("课堂 {} 为 {} 模式，按 C5 不开启 WebRTC/RTMP", lessonId, lesson.getLiveMode());
            return LiveConfigVO.slideOnly(lessonId);
        }

        LiveRecord record = liveRecordMapper.selectByLessonId(lessonId);
        if (record == null) {
            String streamKey = "lesson-" + lessonId + "-" + UUID.randomUUID().toString().substring(0, 8);
            record = LiveRecord.builder()
                    .lessonId(lessonId)
                    .streamKey(streamKey)
                    .pushUrl(rtmpBase + "/" + streamKey)
                    .playUrl(hlsBase + "/" + streamKey + ".m3u8")
                    .durationSec(0)
                    .status(STATUS_PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            liveRecordMapper.insert(record);
            log.info("课堂 {} ONLINE_CLASS 开启直播，streamKey={}", lessonId, streamKey);
        }
        return toOnlineConfig(lessonId, record);
    }

    @Override
    public LiveConfigVO getLiveConfig(Long lessonId) {
        LessonLiveInfo lesson = requireLesson(lessonId);
        if (!MODE_ONLINE_CLASS.equals(lesson.getLiveMode())) {
            return LiveConfigVO.slideOnly(lessonId);
        }
        LiveRecord record = liveRecordMapper.selectByLessonId(lessonId);
        if (record == null) {
            // ONLINE_CLASS 但尚未开播：允许 WebRTC，但暂无推/拉流地址
            return new LiveConfigVO(lessonId, MODE_ONLINE_CLASS, true, true, null, null, null, null);
        }
        return toOnlineConfig(lessonId, record);
    }

    @Override
    public LiveConfigVO stopLive(Long lessonId, Long operatorId) {
        LiveRecord record = liveRecordMapper.selectByLessonId(lessonId);
        if (record == null) {
            // SLIDE_ONLY 或未开播：无直播可结束，幂等返回
            return LiveConfigVO.slideOnly(lessonId);
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
        record.setStatus(STATUS_ENDED);
        log.info("课堂 {} 直播结束", lessonId);
        return toOnlineConfig(lessonId, record);
    }

    private LessonLiveInfo requireLesson(Long lessonId) {
        LessonLiveInfo lesson = lessonLiveMapper.selectLiveInfo(lessonId);
        if (lesson == null) {
            throw new BizException(ErrorCode.LESSON_NOT_FOUND);
        }
        return lesson;
    }

    private LiveConfigVO toOnlineConfig(Long lessonId, LiveRecord r) {
        return new LiveConfigVO(lessonId, MODE_ONLINE_CLASS, true, true,
                r.getStreamKey(), r.getPushUrl(), r.getPlayUrl(), r.getStatus());
    }
}
