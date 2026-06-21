package cn.smu.edu.user.controller;

import cn.smu.edu.common.result.Result;
import cn.smu.edu.user.domain.vo.RoleVO;
import cn.smu.edu.user.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 角色鉴权由 edu-gateway 统一处理，此微服务信任网关已通过 JWT 校验
@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "获取角色列表")
    @GetMapping
    public Result<List<RoleVO>> listRoles() {
        return Result.ok(roleService.listRoles());
    }
}
