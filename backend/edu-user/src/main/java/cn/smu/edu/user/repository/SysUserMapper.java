package cn.smu.edu.user.repository;

import cn.smu.edu.user.domain.entity.SysUser;
import cn.smu.edu.user.domain.vo.UserVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    IPage<UserVO> selectUserPage(
            Page<UserVO> page,
            @Param("keyword") String keyword,
            @Param("userType") Integer userType,
            @Param("deptId") Long deptId,
            @Param("status") Integer status);

    UserVO selectUserVOById(@Param("userId") Long userId);
}
