package cn.smu.edu.auth.service;

import cn.smu.edu.auth.domain.vo.TokenVO;

public interface WechatAuthService {

    /**
     * 小程序登录：code 换 openId，查绑定用户，签发 JWT
     */
    TokenVO loginByMiniProgram(String code);
}
