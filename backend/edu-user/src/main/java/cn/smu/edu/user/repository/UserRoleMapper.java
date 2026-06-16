package cn.smu.edu.user.repository;

import cn.smu.edu.user.domain.entity.UserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    @Select("SELECT role_code FROM user_role WHERE user_id = #{userId}")
    List<String> selectRolesByUserId(@Param("userId") Long userId);
}
