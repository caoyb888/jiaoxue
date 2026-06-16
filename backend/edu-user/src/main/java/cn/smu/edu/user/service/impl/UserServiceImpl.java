package cn.smu.edu.user.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.user.domain.dto.UserCreateDTO;
import cn.smu.edu.user.domain.dto.UserQueryDTO;
import cn.smu.edu.user.domain.dto.UserUpdateDTO;
import cn.smu.edu.user.domain.entity.SysUser;
import cn.smu.edu.user.domain.entity.UserRole;
import cn.smu.edu.user.domain.vo.UserVO;
import cn.smu.edu.user.repository.SysUserMapper;
import cn.smu.edu.user.repository.UserRoleMapper;
import cn.smu.edu.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public IPage<UserVO> pageUsers(UserQueryDTO query) {
        Page<UserVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<UserVO> result = userMapper.selectUserPage(
                page, query.getKeyword(), query.getUserType(),
                query.getDeptId(), query.getStatus());
        result.getRecords().forEach(vo -> {
            List<String> roles = userRoleMapper.selectRolesByUserId(vo.getId());
            vo.setRoles(roles);
        });
        return result;
    }

    @Override
    public UserVO getUserById(Long userId) {
        UserVO vo = userMapper.selectUserVOById(userId);
        if (vo == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        vo.setRoles(userRoleMapper.selectRolesByUserId(userId));
        return vo;
    }

    @Override
    public UserVO getUserByPhone(String phoneCipher) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhoneCipher, phoneCipher)
                .eq(SysUser::getIsDeleted, 0));
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return getUserById(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateDTO dto) {
        long existCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername())
                .eq(SysUser::getIsDeleted, 0));
        if (existCount > 0) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS);
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setRealName(dto.getRealName());
        user.setUserType(dto.getUserType());
        user.setDeptId(dto.getDeptId());
        user.setStatus(1);

        userMapper.insert(user);
        log.info("用户创建成功: userId={}, username={}, userType={}", user.getId(), user.getUsername(), user.getUserType());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, UserUpdateDTO dto) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (dto.getRealName() != null) user.setRealName(dto.getRealName());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getAvatarUrl() != null) user.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());
        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        user.setStatus(0);
        userMapper.updateById(user);
        log.info("用户已禁用: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        user.setStatus(1);
        userMapper.updateById(user);
        log.info("用户已启用: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRole(Long userId, String roleCode, Long deptId) {
        long existCount = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleCode, roleCode)
                .eq(deptId != null, UserRole::getDeptId, deptId));
        if (existCount > 0) {
            return;
        }
        UserRole role = new UserRole();
        role.setUserId(userId);
        role.setRoleCode(roleCode);
        role.setDeptId(deptId);
        userRoleMapper.insert(role);
        log.info("角色授权成功: userId={}, roleCode={}, deptId={}", userId, roleCode, deptId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRole(Long userId, String roleCode) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleCode, roleCode));
    }
}
