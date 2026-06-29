package cn.smu.edu.file.service.impl;

import cn.smu.edu.file.domain.entity.FileObject;
import cn.smu.edu.file.repository.FileObjectMapper;
import cn.smu.edu.file.service.FileLifecycleService;
import cn.smu.edu.file.util.MinioUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileLifecycleServiceImplTest {

    @Mock
    FileObjectMapper fileObjectMapper;
    @Mock
    MinioUtil minioUtil;

    FileLifecycleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FileLifecycleServiceImpl(fileObjectMapper, minioUtil);
        ReflectionTestUtils.setField(service, "replayColdDays", 60);
        ReflectionTestUtils.setField(service, "examAttachDeleteDays", 90);
    }

    private FileObject file(long id, String biz) {
        FileObject f = new FileObject();
        f.setId(id);
        f.setBizType(biz);
        f.setBucketName("edu-files");
        f.setObjectPath("p/" + id);
        return f;
    }

    @Test
    @SuppressWarnings("unchecked")
    void runLifecycleCheck_shouldArchiveReplaysAndPurgeExamAttachments() {
        when(fileObjectMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(file(1, "VIDEO"), file(2, "VIDEO")))  // 第一次：转冷候选
                .thenReturn(List.of(file(3, "EXAM_ATTACH")));            // 第二次：删除候选
        when(fileObjectMapper.markCold(any())).thenReturn(1);
        when(fileObjectMapper.markDeleted(any())).thenReturn(1);

        FileLifecycleService.Result r = service.runLifecycleCheck();

        assertThat(r.coldCount()).isEqualTo(2);
        assertThat(r.deletedCount()).isEqualTo(1);
        verify(fileObjectMapper).markCold(1L);
        verify(fileObjectMapper).markCold(2L);
        verify(minioUtil).deleteObject("edu-files", "p/3"); // 先删 MinIO
        verify(fileObjectMapper).markDeleted(3L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void purge_minioDeleteFailure_shouldStillMarkDeleted() {
        when(fileObjectMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of())                          // 无转冷
                .thenReturn(List.of(file(3, "EXAM_ATTACH")));
        doThrow(new RuntimeException("minio down")).when(minioUtil).deleteObject(any(), any());
        when(fileObjectMapper.markDeleted(any())).thenReturn(1);

        FileLifecycleService.Result r = service.runLifecycleCheck();

        assertThat(r.deletedCount()).isEqualTo(1); // MinIO 失败仍标记 DB 删除
        verify(fileObjectMapper).markDeleted(3L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void runLifecycleCheck_noCandidates_shouldDoNothing() {
        when(fileObjectMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of()).thenReturn(List.of());

        FileLifecycleService.Result r = service.runLifecycleCheck();

        assertThat(r.coldCount()).isZero();
        assertThat(r.deletedCount()).isZero();
        verify(fileObjectMapper, never()).markCold(any());
        verify(minioUtil, never()).deleteObject(any(), any());
    }
}
