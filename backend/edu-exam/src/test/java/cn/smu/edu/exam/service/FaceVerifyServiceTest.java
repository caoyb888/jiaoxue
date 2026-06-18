package cn.smu.edu.exam.service;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.exam.client.BaiduFaceClient;
import cn.smu.edu.exam.config.BaiduFaceConfig;
import cn.smu.edu.exam.domain.dto.FaceVerifyDTO;
import cn.smu.edu.exam.domain.entity.ExamMonitor;
import cn.smu.edu.exam.domain.entity.ExamPublish;
import cn.smu.edu.exam.domain.vo.FaceVerifyResultVO;
import cn.smu.edu.exam.repository.ExamMonitorMapper;
import cn.smu.edu.exam.repository.ExamPublishMapper;
import cn.smu.edu.exam.service.impl.FaceVerifyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FaceVerifyServiceTest {

    @Mock private ExamPublishMapper publishMapper;
    @Mock private ExamMonitorMapper monitorMapper;
    @Mock private BaiduFaceClient baiduFaceClient;
    @Mock private ArchivePhotoService archivePhotoService;
    @Mock private BaiduFaceConfig baiduFaceConfig;

    @InjectMocks private FaceVerifyServiceImpl service;

    private ExamPublish publish;
    private ExamMonitor monitor;
    private FaceVerifyDTO dto;

    @BeforeEach
    void setUp() {
        publish = new ExamPublish();
        publish.setId(10L);
        publish.setFaceVerifyType(1); // 核验类型：证件照

        monitor = new ExamMonitor();
        monitor.setId(1L);
        monitor.setPublishId(10L);
        monitor.setStudentId(99L);
        monitor.setSessionStatus("VERIFYING");

        dto = new FaceVerifyDTO();
        dto.setLivePhotoBase64("LIVE_PHOTO_BASE64");

        when(baiduFaceConfig.getPassThreshold()).thenReturn(80.0);
    }

    // ── 通过场景 ────────────────────────────────────────────────────────────────

    @Test
    void verify_shouldPassAndUpdateStatusToAnswering_whenScoreAboveThreshold() {
        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(monitorMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(monitor);
        when(archivePhotoService.getDecryptedArchivePhoto(99L)).thenReturn("ARCHIVE_PHOTO");
        when(baiduFaceClient.match("LIVE_PHOTO_BASE64", "ARCHIVE_PHOTO"))
                .thenReturn(new BaiduFaceClient.FaceMatchResult(true, 92.5));

        FaceVerifyResultVO result = service.verify(10L, 99L, dto);

        assertThat(result.getPassed()).isTrue();
        assertThat(result.getScore()).isEqualByComparingTo("92.5");
        assertThat(result.getSessionStatus()).isEqualTo("ANSWERING");
        assertThat(result.getMessage()).contains("通过");

        assertThat(monitor.getFaceVerifyPassed()).isEqualTo(1);
        assertThat(monitor.getFaceVerifyScore()).isEqualByComparingTo("92.5");
        assertThat(monitor.getSessionStatus()).isEqualTo("ANSWERING");
        verify(monitorMapper).updateById(monitor);
    }

    @Test
    void verify_shouldFailAndKeepVerifyingStatus_whenScoreBelowThreshold() {
        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(monitorMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(monitor);
        when(archivePhotoService.getDecryptedArchivePhoto(99L)).thenReturn("ARCHIVE_PHOTO");
        when(baiduFaceClient.match("LIVE_PHOTO_BASE64", "ARCHIVE_PHOTO"))
                .thenReturn(new BaiduFaceClient.FaceMatchResult(false, 45.0));

        FaceVerifyResultVO result = service.verify(10L, 99L, dto);

        assertThat(result.getPassed()).isFalse();
        assertThat(result.getScore()).isEqualByComparingTo("45.0");
        assertThat(result.getSessionStatus()).isEqualTo("VERIFYING");
        assertThat(result.getMessage()).contains("未通过");

        assertThat(monitor.getFaceVerifyPassed()).isEqualTo(0);
        assertThat(monitor.getSessionStatus()).isEqualTo("VERIFYING");
        verify(monitorMapper).updateById(monitor);
    }

    // ── 边界场景 ────────────────────────────────────────────────────────────────

    @Test
    void verify_shouldThrow_whenExamNotFound() {
        when(publishMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.verify(99L, 1L, dto))
                .isInstanceOf(BizException.class);
        verifyNoInteractions(baiduFaceClient);
    }

    @Test
    void verify_shouldThrow_whenFaceVerifyNotRequired() {
        publish.setFaceVerifyType(0);
        when(publishMapper.selectById(10L)).thenReturn(publish);
        assertThatThrownBy(() -> service.verify(10L, 99L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未开启人脸核验");
        verifyNoInteractions(baiduFaceClient);
    }

    @Test
    void verify_shouldThrow_whenFaceVerifyTypeIsNull() {
        publish.setFaceVerifyType(null);
        when(publishMapper.selectById(10L)).thenReturn(publish);
        assertThatThrownBy(() -> service.verify(10L, 99L, dto))
                .isInstanceOf(BizException.class);
        verifyNoInteractions(baiduFaceClient);
    }

    @Test
    void verify_shouldThrow_whenMonitorNotFound() {
        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(monitorMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(null);
        assertThatThrownBy(() -> service.verify(10L, 99L, dto))
                .isInstanceOf(BizException.class);
        verifyNoInteractions(baiduFaceClient);
    }

    @Test
    void verify_shouldThrow_whenAlreadySubmitted() {
        monitor.setSessionStatus("SUBMITTED");
        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(monitorMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(monitor);
        assertThatThrownBy(() -> service.verify(10L, 99L, dto))
                .isInstanceOf(BizException.class);
        verifyNoInteractions(baiduFaceClient);
    }

    @Test
    void verify_shouldThrow_whenArchivePhotoNotFound() {
        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(monitorMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(monitor);
        when(archivePhotoService.getDecryptedArchivePhoto(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.verify(10L, 99L, dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("无档案照");
        verifyNoInteractions(baiduFaceClient);
    }

    @Test
    void verify_shouldNeverStoreRawPhoto_c6Compliance() {
        // C6 合规验证：确认 monitor 更新中不包含任何照片数据
        when(publishMapper.selectById(10L)).thenReturn(publish);
        when(monitorMapper.selectByPublishAndStudent(10L, 99L)).thenReturn(monitor);
        when(archivePhotoService.getDecryptedArchivePhoto(99L)).thenReturn("ARCHIVE");
        when(baiduFaceClient.match(any(), any()))
                .thenReturn(new BaiduFaceClient.FaceMatchResult(true, 88.0));

        service.verify(10L, 99L, dto);

        // ExamMonitor 中只有 faceVerifyPassed 和 faceVerifyScore，无任何 photo 字段
        assertThat(monitor.getFaceVerifyPassed()).isNotNull();
        assertThat(monitor.getFaceVerifyScore()).isNotNull();
        assertThat(monitor.getSnapshotUrl()).isNull(); // 仅存截图举证URL，不存比对照
    }
}
