package cn.smu.edu.file.init;

import cn.smu.edu.file.config.MinioProperties;
import cn.smu.edu.file.util.MinioUtil;
import io.minio.MinioClient;
import io.minio.SetBucketLifecycleArgs;
import io.minio.messages.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

/**
 * 应用启动时初始化 MinIO Bucket 及生命周期规则：
 *   edu-live-replay   → 60天后转冷存储（降成本）
 *   edu-exam-attach   → 90天后删除（考后清理）
 *   edu-slides        → 永久保留（课件归档）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BucketInitializer implements ApplicationRunner {

    private final MinioUtil minioUtil;
    private final MinioProperties props;
    private final MinioClient minioClient;

    @Override
    public void run(ApplicationArguments args) {
        log.info("初始化 MinIO Bucket...");
        for (String bucket : props.getBuckets()) {
            minioUtil.createBucket(bucket);
        }
        applyLifecycleRules();
        log.info("MinIO Bucket 初始化完成，共 {} 个", props.getBuckets().size());
    }

    private void applyLifecycleRules() {
        applyExamAttachLifecycle();
        applyLiveReplayLifecycle();
    }

    private void applyExamAttachLifecycle() {
        try {
            List<LifecycleRule> rules = new LinkedList<>();
            rules.add(new LifecycleRule(
                    Status.ENABLED,
                    null,
                    new Expiration((ZonedDateTime) null, 90, null),
                    new RuleFilter("exam/"),
                    "exam-attach-expire-90d",
                    null, null, null
            ));
            minioClient.setBucketLifecycle(SetBucketLifecycleArgs.builder()
                    .bucket("edu-exam-attach")
                    .config(new LifecycleConfiguration(rules))
                    .build());
            log.info("edu-exam-attach 生命周期规则已设置（90天过期）");
        } catch (Exception e) {
            log.warn("设置 edu-exam-attach 生命周期规则失败（不影响启动）: {}", e.getMessage());
        }
    }

    private void applyLiveReplayLifecycle() {
        try {
            List<LifecycleRule> rules = new LinkedList<>();
            rules.add(new LifecycleRule(
                    Status.ENABLED,
                    null,
                    new Expiration((ZonedDateTime) null, 180, null),
                    new RuleFilter("replay/"),
                    "live-replay-expire-180d",
                    null, null, null
            ));
            minioClient.setBucketLifecycle(SetBucketLifecycleArgs.builder()
                    .bucket("edu-live-replay")
                    .config(new LifecycleConfiguration(rules))
                    .build());
            log.info("edu-live-replay 生命周期规则已设置（180天过期）");
        } catch (Exception e) {
            log.warn("设置 edu-live-replay 生命周期规则失败（不影响启动）: {}", e.getMessage());
        }
    }
}
