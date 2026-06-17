package cn.smu.edu.interaction.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AttendDTO {

    /** 口令（与 qrToken 二选一） */
    private String code;

    /** 扫码 Token（与 code 二选一） */
    private String qrToken;

    /** 签到 IP（网关注入，客户端不传） */
    private String ipAddress;
}
