package cn.smu.edu.file.config;

import io.minio.messages.Expiration;
import io.minio.messages.LifecycleConfiguration;
import io.minio.messages.LifecycleRule;
import io.minio.messages.RuleFilter;
import io.minio.messages.Status;
import io.minio.messages.Transition;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MinIO Bucket 生命周期规则定义（S8-07）。三条规则：
 * <ul>
 *   <li>{@code edu-live-replay} → 60 天转冷存储（Transition→COLD，降成本）；</li>
 *   <li>{@code edu-exam-attach} → 90 天过期删除（考后清理）；</li>
 *   <li>{@code edu-slides}     → 365 天归档转冷（课件长期保留但降成本）。</li>
 * </ul>
 *
 * <p>纯规则构建（无 IO），便于单测；实际下发见 {@link cn.smu.edu.file.init.BucketInitializer}。
 * Transition 需 MinIO 配置 ILM tier（{@code mc ilm tier add}），dev 单机无 tier 时下发会被拒、
 * 由调用方 try/catch 降级，不影响启动。
 */
public final class BucketLifecycleRules {

    public static final String BUCKET_LIVE_REPLAY = "edu-live-replay";
    public static final String BUCKET_EXAM_ATTACH = "edu-exam-attach";
    public static final String BUCKET_SLIDES = "edu-slides";

    public static final String COLD_STORAGE_CLASS = "COLD";

    public static final int REPLAY_COLD_DAYS = 60;
    public static final int EXAM_ATTACH_EXPIRE_DAYS = 90;
    public static final int SLIDES_ARCHIVE_DAYS = 365;

    private BucketLifecycleRules() {
    }

    /** bucket → 生命周期配置（按规范三条）。 */
    public static Map<String, LifecycleConfiguration> rulesByBucket() {
        Map<String, LifecycleConfiguration> map = new LinkedHashMap<>();
        map.put(BUCKET_LIVE_REPLAY, transitionToCold("live-replay-cold-60d", "replay/", REPLAY_COLD_DAYS));
        map.put(BUCKET_EXAM_ATTACH, expire("exam-attach-expire-90d", "exam/", EXAM_ATTACH_EXPIRE_DAYS));
        map.put(BUCKET_SLIDES, transitionToCold("slides-archive-365d", "", SLIDES_ARCHIVE_DAYS));
        return map;
    }

    /** N 天后转冷存储（Transition→COLD）。 */
    static LifecycleConfiguration transitionToCold(String id, String prefix, int days) {
        LifecycleRule rule = new LifecycleRule(
                Status.ENABLED,
                null,
                null,
                new RuleFilter(prefix),
                id,
                null,
                null,
                new Transition((ZonedDateTime) null, days, COLD_STORAGE_CLASS));
        return new LifecycleConfiguration(List.of(rule));
    }

    /** N 天后过期删除。 */
    static LifecycleConfiguration expire(String id, String prefix, int days) {
        LifecycleRule rule = new LifecycleRule(
                Status.ENABLED,
                null,
                new Expiration((ZonedDateTime) null, days, null),
                new RuleFilter(prefix),
                id,
                null,
                null,
                null);
        return new LifecycleConfiguration(List.of(rule));
    }
}
