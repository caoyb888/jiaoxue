package cn.smu.edu.user.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.user.domain.dto.UserCreateDTO;
import cn.smu.edu.user.domain.dto.UserQueryDTO;
import cn.smu.edu.user.domain.dto.UserUpdateDTO;
import cn.smu.edu.user.domain.vo.UserVO;
import cn.smu.edu.user.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPT_ADMIN')")
    public Result<IPage<UserVO>> pageUsers(UserQueryDTO query) {
        return Result.ok(userService.pageUsers(query));
    }

    @Operation(summary = "查询用户详情")
    @GetMapping("/{userId}")
    public Result<UserVO> getUserById(@PathVariable Long userId) {
        return Result.ok(userService.getUserById(userId));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @OperationLog(module = "user", operation = "创建用户")
    public Result<Long> createUser(@Valid @RequestBody UserCreateDTO dto) {
        return Result.ok(userService.createUser(dto));
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/{userId}")
    @OperationLog(module = "user", operation = "更新用户信息")
    public Result<Void> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateDTO dto) {
        userService.updateUser(userId, dto);
        return Result.ok();
    }

    @Operation(summary = "禁用用户")
    @PutMapping("/{userId}/disable")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @OperationLog(module = "user", operation = "禁用用户")
    public Result<Void> disableUser(@PathVariable Long userId) {
        userService.disableUser(userId);
        return Result.ok();
    }

    @Operation(summary = "启用用户")
    @PutMapping("/{userId}/enable")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @OperationLog(module = "user", operation = "启用用户")
    public Result<Void> enableUser(@PathVariable Long userId) {
        userService.enableUser(userId);
        return Result.ok();
    }

    @Operation(summary = "分配角色")
    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @OperationLog(module = "user", operation = "分配角色")
    public Result<Void> assignRole(
            @PathVariable Long userId,
            @RequestParam String roleCode,
            @RequestParam(required = false) Long deptId) {
        userService.assignRole(userId, roleCode, deptId);
        return Result.ok();
    }

    @Operation(summary = "撤销角色")
    @DeleteMapping("/{userId}/roles/{roleCode}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @OperationLog(module = "user", operation = "撤销角色")
    public Result<Void> removeRole(
            @PathVariable Long userId,
            @PathVariable String roleCode) {
        userService.removeRole(userId, roleCode);
        return Result.ok();
    }
}
