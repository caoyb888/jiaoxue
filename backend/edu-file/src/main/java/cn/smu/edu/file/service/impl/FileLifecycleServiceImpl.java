package cn.smu.edu.file.service.impl;

import cn.smu.edu.file.domain.entity.FileObject;
import cn.smu.edu.file.repository.FileObjectMapper;
import cn.smu.edu.file.service.FileLifecycleService;
import cn.smu.edu.file.util.MinioUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@link FileLifecycleService} 实现。
 *
 * <p>转冷存储仅改 DB（storage_class=COLD）；MinIO 实际跨桶/分层转储由 S8-07 桶生命周期规则负责。
 * 删除则先移除 MinIO 对象再标记 DB。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileLifecycleServiceImpl implements FileLifecycleService {

    private static final String BIZ_VIDEO = "VIDEO";          // 直播回放/录播
    private static final String BIZ_EXAM_ATTACH = "EXAM_ATTACH";
    private static final String STORAGE_HOT = "HOT";

    /** 直播回放转冷存储天数。 */
    @Value("${file.lifecycle.replay-cold-days:60}")
    private int replayColdDays;

    /** 考试附件删除天数。 */
    @Value("${file.lifecycle.exam-attach-delete-days:90}")
    private int examAttachDeleteDays;

    private final FileObjectMapper fileObjectMapper;
    private final MinioUtil minioUtil;

    @Override
    public Result runLifecycleCheck() {
        int cold = archiveReplays();
        int deleted = purgeExamAttachments();
        log.info("文件生命周期检查完成: 转冷存储={}, 删除={}", cold, deleted);
        return new Result(cold, deleted);
    }

    /** 直播回放超 {@link #replayColdDays} 天且仍 HOT → 转冷存储。 */
    private int archiveReplays() {
        LocalDateTime before = LocalDateTime.now().minusDays(replayColdDays);
        List<FileObject> candidates = fileObjectMapper.selectList(new LambdaQueryWrapper<FileObject>()
                .eq(FileObject::getBizType, BIZ_VIDEO)
                .eq(FileObject::getStorageClass, STORAGE_HOT)
                .lt(FileObject::getCreatedAt, before));
        int count = 0;
        for (FileObject f : candidates) {
            count += fileObjectMapper.markCold(f.getId());
        }
        if (count > 0) {
            log.info("直播回放转冷存储: {} 个（超 {} 天）", count, replayColdDays);
        }
        return count;
    }

    /** 考试附件超 {@link #examAttachDeleteDays} 天 → 删 MinIO 对象 + 标记删除。 */
    private int purgeExamAttachments() {
        LocalDateTime before = LocalDateTime.now().minusDays(examAttachDeleteDays);
        List<FileObject> candidates = fileObjectMapper.selectList(new LambdaQueryWrapper<FileObject>()
                .eq(FileObject::getBizType, BIZ_EXAM_ATTACH)
                .lt(FileObject::getCreatedAt, before));
        int count = 0;
        for (FileObject f : candidates) {
            try {
                minioUtil.deleteObject(f.getBucketName(), f.getObjectPath());
            } catch (Exception e) {
                // MinIO 删除失败（对象可能已不存在）：仍标记 DB 删除，避免反复重试
                log.warn("删除 MinIO 对象失败，仍标记 DB 删除: bucket={}, object={}, err={}",
                        f.getBucketName(), f.getObjectPath(), e.getMessage());
            }
            count += fileObjectMapper.markDeleted(f.getId());
        }
        if (count > 0) {
            log.info("考试附件删除: {} 个（超 {} 天）", count, examAttachDeleteDays);
        }
        return count;
    }
}
