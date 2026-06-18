package cn.smu.edu.exam.client.impl;

import cn.smu.edu.exam.client.BaiduFaceClient;
import cn.smu.edu.exam.config.BaiduFaceConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 百度 AI 人脸比对实现。
 * 调用流程：先获取 Access Token（Bearer 模式），再调用 /face/v3/match 比对接口。
 * Token 本地缓存，提前 60s 刷新，减少重复请求。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BaiduFaceClientImpl implements BaiduFaceClient {

    private final BaiduFaceConfig config;
    private final RestTemplate restTemplate;

    private volatile String cachedToken;
    private volatile long tokenExpiresAt = 0L;

    @Override
    public FaceMatchResult match(String livePhotoBase64, String archivePhotoBase64) {
        String token = getAccessToken();
        String url = config.getMatchUrl() + "?access_token=" + token;

        List<Map<String, String>> body = List.of(
                Map.of("image", livePhotoBase64,    "image_type", "BASE64", "face_type", "LIVE"),
                Map.of("image", archivePhotoBase64, "image_type", "BASE64", "face_type", "CERT")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                url, new HttpEntity<>(body, headers), Map.class);

        if (response == null) {
            log.error("百度人脸比对接口返回 null");
            return new FaceMatchResult(false, 0.0);
        }

        int errorCode = ((Number) response.getOrDefault("error_code", -1)).intValue();
        if (errorCode != 0) {
            log.warn("百度人脸比对失败: error_code={}, error_msg={}",
                    errorCode, response.get("error_msg"));
            return new FaceMatchResult(false, 0.0);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        double score = result != null
                ? ((Number) result.getOrDefault("score", 0)).doubleValue()
                : 0.0;

        boolean matched = score >= config.getPassThreshold();
        log.info("人脸比对完成: score={}, threshold={}, matched={}", score, config.getPassThreshold(), matched);
        return new FaceMatchResult(matched, score);
    }

    private String getAccessToken() {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiresAt - 60_000L) {
            return cachedToken;
        }
        String url = String.format("%s?grant_type=client_credentials&client_id=%s&client_secret=%s",
                config.getTokenUrl(), config.getApiKey(), config.getSecretKey());

        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
        if (resp == null || !resp.containsKey("access_token")) {
            throw new IllegalStateException("获取百度 Access Token 失败");
        }
        cachedToken = (String) resp.get("access_token");
        int expiresIn = ((Number) resp.getOrDefault("expires_in", 2592000)).intValue();
        tokenExpiresAt = System.currentTimeMillis() + expiresIn * 1000L;
        log.info("百度 Access Token 已刷新，有效期 {}s", expiresIn);
        return cachedToken;
    }
}
