package cn.smu.edu.auth.service;

import cn.smu.edu.auth.domain.dto.SmsLoginDTO;
import cn.smu.edu.auth.domain.vo.TokenVO;

public interface AuthService {
    TokenVO loginByPhone(SmsLoginDTO dto);
    TokenVO refreshToken(String refreshToken);
}
