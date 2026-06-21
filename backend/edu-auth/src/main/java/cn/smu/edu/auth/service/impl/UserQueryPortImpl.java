package cn.smu.edu.auth.service.impl;

import cn.smu.edu.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserQueryPortImpl implements UserQueryPort {

    private final RestTemplate restTemplate;

    @Value("${edu.user.service-url:http://100.84.68.115:18082}")
    private String userServiceUrl;

    @Override
    public UserInfo findByPhone(String phone) {
        try {
            String url = userServiceUrl + "/internal/users/by-phone?phoneCipher=" + phone;
            ResponseEntity<Result<UserInfo>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});
            Result<UserInfo> body = resp.getBody();
            if (body != null && body.code() == 200) {
                return body.data();
            }
            return null;
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            log.error("查询用户失败: phone={}****, error={}", phone.substring(0, Math.min(3, phone.length())), e.getMessage());
            return null;
        }
    }

    @Override
    public UserInfo findById(Long userId) {
        try {
            String url = userServiceUrl + "/internal/users/" + userId;
            ResponseEntity<Result<UserInfo>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});
            Result<UserInfo> body = resp.getBody();
            if (body != null && body.code() == 200) {
                return body.data();
            }
            return null;
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            log.error("查询用户失败: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }

    @Override
    public UserInfo findByWechatOpenId(String openId) {
        try {
            String url = userServiceUrl + "/internal/users/by-openid?openId=" + openId;
            ResponseEntity<Result<UserInfo>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});
            Result<UserInfo> body = resp.getBody();
            if (body != null && body.code() == 200) {
                return body.data();
            }
            return null;
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            log.error("查询用户失败: openId={}, error={}", openId, e.getMessage());
            return null;
        }
    }
}
