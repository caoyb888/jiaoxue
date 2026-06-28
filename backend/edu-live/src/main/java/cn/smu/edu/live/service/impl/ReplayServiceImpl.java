package cn.smu.edu.live.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.live.domain.dto.LessonLiveInfo;
import cn.smu.edu.live.domain.entity.LiveRecord;
import cn.smu.edu.live.domain.vo.ReplayVO;
import cn.smu.edu.live.repository.LessonLiveMapper;
import cn.smu.edu.live.repository.LiveRecordMapper;
import cn.smu.edu.live.service.ReplayService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * {@link ReplayService} 实现。回放就绪 = live_record.status=3 且 replay_path 非空。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReplayServiceImpl implements ReplayService {

    private static final int STATUS_REPLAY_READY = 3;
    private static final int PRESIGN_EXPIRE_MINUTES = 120;

    @Value("${live.replay-bucket:live-replay}")
    private String replayBucket;

    /** 回放 CDN 基地址；配置则用 CDN 直链，否则用 MinIO 预签名 URL。 */
    @Value("${live.replay-cdn-base:}")
    private String replayCdnBase;

    private final LiveRecordMapper liveRecordMapper;
    private final LessonLiveMapper lessonLiveMapper;
    private final MinioClient minioClient;

    @Override
    public ReplayVO getReplay(Long lessonId, String roles) {
        LessonLiveInfo lesson = lessonLiveMapper.selectLiveInfo(lessonId);
        if (lesson == null) {
            throw new BizException(ErrorCode.LESSON_NOT_FOUND);
        }

        // replay_visible=0 时学生不可见；教师/管理员不受限
        boolean staff = isStaff(roles);
        boolean visible = staff || Integer.valueOf(1).equals(lesson.getReplayVisible());
        if (!visible) {
            return ReplayVO.hidden(lessonId);
        }

        LiveRecord record = liveRecordMapper.selectByLessonId(lessonId);
        if (record == null
                || !Integer.valueOf(STATUS_REPLAY_READY).equals(record.getStatus())
                || !StringUtils.hasText(record.getReplayPath())) {
            return ReplayVO.notReady(lessonId);
        }

        String url = buildReplayUrl(record.getReplayPath());
        return new ReplayVO(lessonId, true, true, url, record.getDurationSec());
    }

    private boolean isStaff(String roles) {
        return roles != null && (roles.contains("ROLE_TEACHER") || roles.contains("ROLE_ADMIN"));
    }

    /** 优先 CDN 直链；否则生成 MinIO 预签名 GET URL（2h 有效）。 */
    private String buildReplayUrl(String replayPath) {
        if (StringUtils.hasText(replayCdnBase)) {
            return replayCdnBase.replaceAll("/+$", "") + "/" + replayPath;
        }
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(replayBucket)
                    .object(replayPath)
                    .expiry(PRESIGN_EXPIRE_MINUTES, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            log.error("生成回放预签名 URL 失败: bucket={}, object={}", replayBucket, replayPath, e);
            throw new BizException(ErrorCode.SYSTEM_ERROR.getCode(), "生成回放链接失败");
        }
    }
}
