package cn.smu.edu.user.service;

import cn.smu.edu.user.domain.vo.RoleVO;

import java.util.List;

public interface RoleService {

    /** 获取系统全部可分配角色。 */
    List<RoleVO> listRoles();
}
