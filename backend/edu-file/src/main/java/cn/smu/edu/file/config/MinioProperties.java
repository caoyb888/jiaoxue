package cn.smu.edu.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    private String endpoint = "http://100.84.68.115:19000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";

    private List<String> buckets = List.of(
            "edu-slides",
            "edu-videos",
            "edu-exam-attach",
            "edu-live-replay",
            "edu-archive-photo"
    );

    private String defaultBucket = "edu-slides";

    private int presignExpireMinutes = 60;
}
