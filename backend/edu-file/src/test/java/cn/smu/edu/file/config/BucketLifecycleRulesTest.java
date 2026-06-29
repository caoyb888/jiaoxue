package cn.smu.edu.file.config;

import io.minio.messages.LifecycleConfiguration;
import io.minio.messages.LifecycleRule;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BucketLifecycleRulesTest {

    @Test
    void rulesByBucket_shouldDefineThreeBuckets() {
        Map<String, LifecycleConfiguration> rules = BucketLifecycleRules.rulesByBucket();
        assertThat(rules).containsOnlyKeys(
                "edu-live-replay", "edu-exam-attach", "edu-slides");
    }

    @Test
    void liveReplay_shouldTransitionToColdAt60Days() {
        LifecycleRule rule = BucketLifecycleRules.rulesByBucket().get("edu-live-replay").rules().get(0);
        assertThat(rule.transition()).isNotNull();
        assertThat(rule.transition().days()).isEqualTo(60);
        assertThat(rule.transition().storageClass()).isEqualTo("COLD");
        assertThat(rule.filter().prefix()).isEqualTo("replay/");
        assertThat(rule.expiration()).isNull(); // 转冷而非删除
    }

    @Test
    void examAttach_shouldExpireAt90Days() {
        LifecycleRule rule = BucketLifecycleRules.rulesByBucket().get("edu-exam-attach").rules().get(0);
        assertThat(rule.expiration()).isNotNull();
        assertThat(rule.expiration().days()).isEqualTo(90);
        assertThat(rule.filter().prefix()).isEqualTo("exam/");
        assertThat(rule.transition()).isNull(); // 删除而非转冷
    }

    @Test
    void slides_shouldArchiveToColdAt365Days() {
        LifecycleRule rule = BucketLifecycleRules.rulesByBucket().get("edu-slides").rules().get(0);
        assertThat(rule.transition()).isNotNull();
        assertThat(rule.transition().days()).isEqualTo(365);
        assertThat(rule.transition().storageClass()).isEqualTo("COLD");
    }
}
