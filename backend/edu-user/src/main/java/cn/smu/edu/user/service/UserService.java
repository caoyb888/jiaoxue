package cn.smu.edu.user.service;

import cn.smu.edu.user.domain.dto.UserCreateDTO;
import cn.smu.edu.user.domain.dto.UserQueryDTO;
import cn.smu.edu.user.domain.dto.UserUpdateDTO;
import cn.smu.edu.user.domain.vo.UserVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface UserService {

    IPage<UserVO> pageUsers(UserQueryDTO query);

    UserVO getUserById(Long userId);

    UserVO getUserByPhone(String phoneCipher);

    Long createUser(UserCreateDTO dto);

    void updateUser(Long userId, UserUpdateDTO dto);

    void disableUser(Long userId);

    void enableUser(Long userId);

    void assignRole(Long userId, String roleCode, Long deptId);

    void removeRole(Long userId, String roleCode);

    /** 全量替换用户角色：roleIds 对应 RoleEnum 的 id（空列表=清空）。 */
    void assignRoles(Long userId, java.util.List<Long> roleIds);
}
