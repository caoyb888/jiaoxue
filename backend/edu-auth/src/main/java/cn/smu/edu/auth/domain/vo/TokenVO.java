package cn.smu.edu.auth.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenVO {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;   // 秒
    private String tokenType; // Bearer
}
