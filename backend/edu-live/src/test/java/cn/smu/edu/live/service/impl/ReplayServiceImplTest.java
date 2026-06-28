package cn.smu.edu.live.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.live.domain.dto.LessonLiveInfo;
import cn.smu.edu.live.domain.entity.LiveRecord;
import cn.smu.edu.live.domain.vo.ReplayVO;
import cn.smu.edu.live.repository.LessonLiveMapper;
import cn.smu.edu.live.repository.LiveRecordMapper;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplayServiceImplTest {

    @Mock
    LiveRecordMapper liveRecordMapper;
    @Mock
    LessonLiveMapper lessonLiveMapper;
    @Mock
    MinioClient minioClient;

    ReplayServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReplayServiceImpl(liveRecordMapper, lessonLiveMapper, minioClient);
        ReflectionTestUtils.setField(service, "replayBucket", "live-replay");
        ReflectionTestUtils.setField(service, "replayCdnBase", "https://cdn.smu.edu.cn/replay");
    }

    private LessonLiveInfo lesson(int replayVisible) {
        LessonLiveInfo l = new LessonLiveInfo();
        l.setLiveMode("ONLINE_CLASS");
        l.setTeacherId(50L);
        l.setReplayVisible(replayVisible);
        return l;
    }

    private LiveRecord readyRecord() {
        return LiveRecord.builder().id(1L).lessonId(2L).status(3)
                .replayPath("replay/2/k.flv").durationSec(1800).build();
    }

    @Test
    void getReplay_studentHidden_shouldReturnHidden() {
        when(lessonLiveMapper.selectLiveInfo(2L)).thenReturn(lesson(0)); // 不可见
        ReplayVO vo = service.getReplay(2L, "ROLE_STUDENT");
        assertThat(vo.visible()).isFalse();
        assertThat(vo.available()).isFalse();
        assertThat(vo.replayUrl()).isNull();
    }

    @Test
    void getReplay_teacherSeesHiddenReplay() {
        when(lessonLiveMapper.selectLiveInfo(2L)).thenReturn(lesson(0)); // 对学生不可见
        when(liveRecordMapper.selectByLessonId(2L)).thenReturn(readyRecord());

        ReplayVO vo = service.getReplay(2L, "ROLE_TEACHER");

        assertThat(vo.visible()).isTrue();
        assertThat(vo.available()).isTrue();
        assertThat(vo.replayUrl()).isEqualTo("https://cdn.smu.edu.cn/replay/replay/2/k.flv");
        assertThat(vo.durationSec()).isEqualTo(1800);
    }

    @Test
    void getReplay_studentVisibleAndReady_shouldReturnUrl() {
        when(lessonLiveMapper.selectLiveInfo(2L)).thenReturn(lesson(1));
        when(liveRecordMapper.selectByLessonId(2L)).thenReturn(readyRecord());

        ReplayVO vo = service.getReplay(2L, "ROLE_STUDENT");

        assertThat(vo.available()).isTrue();
        assertThat(vo.replayUrl()).contains("replay/2/k.flv");
    }

    @Test
    void getReplay_notReady_shouldReturnNotReady() {
        when(lessonLiveMapper.selectLiveInfo(2L)).thenReturn(lesson(1));
        LiveRecord living = LiveRecord.builder().id(1L).lessonId(2L).status(1).build(); // 推流中，无回放
        when(liveRecordMapper.selectByLessonId(2L)).thenReturn(living);

        ReplayVO vo = service.getReplay(2L, "ROLE_STUDENT");

        assertThat(vo.available()).isFalse();
        assertThat(vo.visible()).isTrue();
        assertThat(vo.replayUrl()).isNull();
    }

    @Test
    void getReplay_lessonNotFound_shouldThrow() {
        when(lessonLiveMapper.selectLiveInfo(404L)).thenReturn(null);
        assertThatThrownBy(() -> service.getReplay(404L, "ROLE_TEACHER")).isInstanceOf(BizException.class);
    }

    @Test
    void getReplay_noCdn_shouldUseMinioPresignedUrl() throws Exception {
        ReflectionTestUtils.setField(service, "replayCdnBase", ""); // 关闭 CDN → 走预签名
        when(lessonLiveMapper.selectLiveInfo(2L)).thenReturn(lesson(1));
        when(liveRecordMapper.selectByLessonId(2L)).thenReturn(readyRecord());
        lenient().when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://minio/live-replay/replay/2/k.flv?sig=x");

        ReplayVO vo = service.getReplay(2L, "ROLE_STUDENT");

        assertThat(vo.replayUrl()).startsWith("http://minio/live-replay/");
    }
}
