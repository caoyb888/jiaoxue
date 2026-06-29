package cn.smu.edu.file.init;

import cn.smu.edu.file.config.BucketLifecycleRules;
import cn.smu.edu.file.config.MinioProperties;
import cn.smu.edu.file.util.MinioUtil;
import io.minio.MinioClient;
import io.minio.SetBucketLifecycleArgs;
import io.minio.messages.LifecycleConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 应用启动时初始化 MinIO Bucket 及生命周期规则（S8-07）：
 * <ul>
 *   <li>edu-live-replay → 60 天转冷存储；</li>
 *   <li>edu-exam-attach → 90 天过期删除；</li>
 *   <li>edu-slides      → 365 天归档转冷。</li>
 * </ul>
 * 规则定义见 {@link BucketLifecycleRules}；dev 单机 MinIO 无 ILM tier 时 Transition 下发
 * 可能被拒，try/catch 降级不影响启动（生产由 OPS 经 {@code infra/scripts/minio-lifecycle.sh} 配 tier）。
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
        for (Map.Entry<String, LifecycleConfiguration> e : BucketLifecycleRules.rulesByBucket().entrySet()) {
            try {
                minioClient.setBucketLifecycle(SetBucketLifecycleArgs.builder()
                        .bucket(e.getKey())
                        .config(e.getValue())
                        .build());
                log.info("MinIO 生命周期规则已设置: bucket={}", e.getKey());
            } catch (Exception ex) {
                log.warn("设置 MinIO 生命周期规则失败（不影响启动，dev 无 tier 属正常）: bucket={}, err={}",
                        e.getKey(), ex.getMessage());
            }
        }
    }
}
