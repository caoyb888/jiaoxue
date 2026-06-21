package cn.smu.edu.user.service.impl;

import cn.smu.edu.user.domain.enums.RoleEnum;
import cn.smu.edu.user.domain.vo.RoleVO;
import cn.smu.edu.user.service.RoleService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    @Override
    public List<RoleVO> listRoles() {
        return Arrays.stream(RoleEnum.values())
                .map(r -> {
                    RoleVO vo = new RoleVO();
                    vo.setId(r.getId());
                    vo.setRoleCode(r.getRoleCode());
                    vo.setRoleName(r.getRoleName());
                    return vo;
                })
                .toList();
    }
}
