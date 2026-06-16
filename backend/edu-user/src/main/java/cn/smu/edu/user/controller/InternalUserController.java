package cn.smu.edu.user.controller;

import cn.smu.edu.common.result.Result;
import cn.smu.edu.user.domain.entity.SysUser;
import cn.smu.edu.user.domain.vo.UserVO;
import cn.smu.edu.user.repository.SysUserMapper;
import cn.smu.edu.user.repository.UserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 服务间内部调用接口（不经过网关，由 edu-auth 通过 OpenFeign 直连调用）
 * 生产环境通过内网IP + Sentinel 防护，禁止公网暴露
 */
@Hidden
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final SysUserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    @GetMapping("/by-phone")
    public Result<UserVO> findByPhoneCipher(@RequestParam String phoneCipher) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhoneCipher, phoneCipher)
                .eq(SysUser::getIsDeleted, 0));
        if (user == null) return Result.fail(404, "用户不存在");
        return Result.ok(toVO(user));
    }

    @GetMapping("/{userId}")
    public Result<UserVO> findById(@PathVariable Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) return Result.fail(404, "用户不存在");
        return Result.ok(toVO(user));
    }

    @GetMapping("/by-openid")
    public Result<UserVO> findByOpenId(@RequestParam String openId) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getOpenId, openId)
                .eq(SysUser::getIsDeleted, 0));
        if (user == null) return Result.fail(404, "用户不存在");
        return Result.ok(toVO(user));
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setStudentNo(user.getStudentNo());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setEmail(user.getEmail());
        vo.setUserType(user.getUserType());
        vo.setDeptId(user.getDeptId());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setStatus(user.getStatus());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setCreatedAt(user.getCreatedAt());
        List<String> roles = userRoleMapper.selectRolesByUserId(user.getId());
        vo.setRoles(roles);
        return vo;
    }
}
