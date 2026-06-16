package cn.smu.edu.auth.controller;

import cn.smu.edu.auth.domain.dto.WechatLoginDTO;
import cn.smu.edu.auth.domain.vo.TokenVO;
import cn.smu.edu.auth.service.WechatAuthService;
import cn.smu.edu.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "微信认证")
@RestController
@RequestMapping("/api/v1/auth/wechat")
@RequiredArgsConstructor
public class WechatAuthController {

    private final WechatAuthService wechatAuthService;

    @Operation(summary = "微信小程序登录（code 换 token）")
    @PostMapping("/miniprogram/login")
    public Result<TokenVO> miniProgramLogin(@Valid @RequestBody WechatLoginDTO dto) {
        return Result.ok(wechatAuthService.loginByMiniProgram(dto.getCode()));
    }
}
