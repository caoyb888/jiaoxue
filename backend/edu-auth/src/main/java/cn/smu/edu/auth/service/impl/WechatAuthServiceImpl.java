package cn.smu.edu.auth.service.impl;

import cn.smu.edu.auth.domain.vo.TokenVO;
import cn.smu.edu.auth.service.WechatAuthService;
import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatAuthServiceImpl implements WechatAuthService {

    private final RestTemplate restTemplate;
    private final UserQueryPort userQueryPort;
    private final cn.smu.edu.auth.service.impl.JwtTokenService jwtTokenService;

    @Value("${edu.wechat.app-id:}")
    private String appId;

    @Value("${edu.wechat.app-secret:}")
    private String appSecret;

    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid={appId}&secret={secret}&js_code={code}&grant_type=authorization_code";

    @Override
    public TokenVO loginByMiniProgram(String code) {
        // 1. code 换 openId（调用微信接口）
        @SuppressWarnings("unchecked")
        Map<String, Object> result = restTemplate.getForObject(
                CODE2SESSION_URL, Map.class,
                Map.of("appId", appId, "secret", appSecret, "code", code));

        if (result == null || result.containsKey("errcode")) {
            log.warn("微信 code2session 失败: result={}", result);
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        String openId = (String) result.get("openid");
        String unionId = (String) result.get("unionid");

        // 2. 查找绑定用户
        UserInfo user = userQueryPort.findByWechatOpenId(openId);
        if (user == null) {
            // 未绑定手机号，返回需要绑定标识（实际项目这里可返回临时 token）
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        if (!user.isEnabled()) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        // 3. 签发 JWT
        TokenVO token = jwtTokenService.issueToken(user);
        log.info("微信小程序登录成功: userId={}", user.getId());
        return token;
    }
}
