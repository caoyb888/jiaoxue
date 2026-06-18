package cn.smu.edu.exam.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "edu.baidu.face")
public class BaiduFaceConfig {

    private String apiKey = "";
    private String secretKey = "";
    /** 人脸相似度通过阈值（0~100），默认 80 */
    private double passThreshold = 80.0;
    private String tokenUrl = "https://aip.baidubce.com/oauth/2.0/token";
    private String matchUrl  = "https://aip.baidubce.com/rest/2.0/face/v3/match";
}
