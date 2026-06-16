package cn.smu.edu.auth.controller;

import cn.smu.edu.auth.domain.dto.SmsLoginDTO;
import cn.smu.edu.auth.domain.vo.TokenVO;
import cn.smu.edu.auth.service.AuthService;
import cn.smu.edu.auth.service.SmsService;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;
    private final SmsService smsService;

    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms/send")
    public Result<Void> sendSms(
            @RequestParam @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone) {
        smsService.sendCode(phone);
        return Result.ok();
    }

    @Operation(summary = "手机号登录")
    @PostMapping("/login/phone")
    public Result<TokenVO> loginByPhone(@Valid @RequestBody SmsLoginDTO dto) {
        return Result.ok(authService.loginByPhone(dto));
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/token/refresh")
    public Result<TokenVO> refreshToken(@RequestParam String refreshToken) {
        return Result.ok(authService.refreshToken(refreshToken));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
        // 网关层已校验 JWT，这里只需清理 refresh token（由客户端传入）
        return Result.ok();
    }
}
