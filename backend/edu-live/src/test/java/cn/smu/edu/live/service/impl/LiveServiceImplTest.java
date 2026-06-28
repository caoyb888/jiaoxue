package cn.smu.edu.live.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.live.domain.dto.LessonLiveInfo;
import cn.smu.edu.live.domain.entity.LiveRecord;
import cn.smu.edu.live.domain.vo.LiveConfigVO;
import cn.smu.edu.live.repository.LessonLiveMapper;
import cn.smu.edu.live.repository.LiveRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveServiceImplTest {

    @Mock
    LiveRecordMapper liveRecordMapper;
    @Mock
    LessonLiveMapper lessonLiveMapper;

    LiveServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LiveServiceImpl(liveRecordMapper, lessonLiveMapper);
        ReflectionTestUtils.setField(service, "rtmpBase", "rtmp://srs:1935/live");
        ReflectionTestUtils.setField(service, "hlsBase", "http://srs:8080/live");
    }

    private LessonLiveInfo lesson(String mode) {
        LessonLiveInfo l = new LessonLiveInfo();
        l.setLiveMode(mode);
        l.setTeacherId(50L);
        l.setStatus(1);
        return l;
    }

    @Test
    void startLive_slideOnly_shouldDisableAllStreamingAndNotCreateRecord() {
        when(lessonLiveMapper.selectLiveInfo(1L)).thenReturn(lesson("SLIDE_ONLY"));

        LiveConfigVO vo = service.startLive(1L, 50L);

        // C5：线下课堂关闭 WebRTC/RTMP，无推拉流地址
        assertThat(vo.liveMode()).isEqualTo("SLIDE_ONLY");
        assertThat(vo.webrtcEnabled()).isFalse();
        assertThat(vo.rtmpEnabled()).isFalse();
        assertThat(vo.pushUrl()).isNull();
        assertThat(vo.playUrl()).isNull();
        assertThat(vo.streamKey()).isNull();
        verify(liveRecordMapper, never()).insert(any(LiveRecord.class));
    }

    @Test
    void startLive_onlineClass_shouldGenerateStreamUrlsAndCreateRecord() {
        when(lessonLiveMapper.selectLiveInfo(2L)).thenReturn(lesson("ONLINE_CLASS"));
        when(liveRecordMapper.selectByLessonId(2L)).thenReturn(null);

        LiveConfigVO vo = service.startLive(2L, 50L);

        assertThat(vo.liveMode()).isEqualTo("ONLINE_CLASS");
        assertThat(vo.webrtcEnabled()).isTrue();
        assertThat(vo.rtmpEnabled()).isTrue();
        assertThat(vo.streamKey()).startsWith("lesson-2-");
        assertThat(vo.pushUrl()).isEqualTo("rtmp://srs:1935/live/" + vo.streamKey());
        assertThat(vo.playUrl()).isEqualTo("http://srs:8080/live/" + vo.streamKey() + ".m3u8");

        ArgumentCaptor<LiveRecord> captor = ArgumentCaptor.forClass(LiveRecord.class);
        verify(liveRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isZero(); // 待推流
        assertThat(captor.getValue().getLessonId()).isEqualTo(2L);
    }

    @Test
    void startLive_onlineClass_shouldReuseExistingRecord() {
        when(lessonLiveMapper.selectLiveInfo(3L)).thenReturn(lesson("ONLINE_CLASS"));
        LiveRecord existing = LiveRecord.builder().id(9L).lessonId(3L)
                .streamKey("lesson-3-abcd1234").pushUrl("rtmp://srs:1935/live/lesson-3-abcd1234")
                .playUrl("http://srs:8080/live/lesson-3-abcd1234.m3u8").status(1).build();
        when(liveRecordMapper.selectByLessonId(3L)).thenReturn(existing);

        LiveConfigVO vo = service.startLive(3L, 50L);

        assertThat(vo.streamKey()).isEqualTo("lesson-3-abcd1234");
        assertThat(vo.status()).isEqualTo(1);
        verify(liveRecordMapper, never()).insert(any(LiveRecord.class));
    }

    @Test
    void startLive_shouldThrowWhenLessonNotFound() {
        when(lessonLiveMapper.selectLiveInfo(404L)).thenReturn(null);
        assertThatThrownBy(() -> service.startLive(404L, 50L)).isInstanceOf(BizException.class);
    }

    @Test
    void getLiveConfig_onlineClassNotStarted_shouldEnableWebrtcWithoutUrls() {
        when(lessonLiveMapper.selectLiveInfo(5L)).thenReturn(lesson("ONLINE_CLASS"));
        when(liveRecordMapper.selectByLessonId(5L)).thenReturn(null);

        LiveConfigVO vo = service.getLiveConfig(5L);

        assertThat(vo.webrtcEnabled()).isTrue();
        assertThat(vo.pushUrl()).isNull();
        assertThat(vo.status()).isNull();
    }

    @Test
    void stopLive_shouldMarkRecordEnded() {
        LiveRecord existing = LiveRecord.builder().id(7L).lessonId(6L)
                .streamKey("k").pushUrl("p").playUrl("pl").status(1).build();
        when(liveRecordMapper.selectByLessonId(6L)).thenReturn(existing);

        LiveConfigVO vo = service.stopLive(6L, 50L);

        assertThat(vo.status()).isEqualTo(2);
        ArgumentCaptor<LiveRecord> captor = ArgumentCaptor.forClass(LiveRecord.class);
        verify(liveRecordMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(2);
        assertThat(captor.getValue().getEndedAt()).isNotNull();
    }

    @Test
    void stopLive_slideOnlyOrNoRecord_shouldBeIdempotent() {
        when(liveRecordMapper.selectByLessonId(8L)).thenReturn(null);
        LiveConfigVO vo = service.stopLive(8L, 50L);
        assertThat(vo.liveMode()).isEqualTo("SLIDE_ONLY");
        verify(liveRecordMapper, never()).updateById(any(LiveRecord.class));
    }
}
