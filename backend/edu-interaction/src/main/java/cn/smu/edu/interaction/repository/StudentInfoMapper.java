package cn.smu.edu.interaction.repository;

import cn.smu.edu.interaction.domain.vo.StudentBriefVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 学生姓名解析（读共享库 edu_db.sys_user）。
 * 说明：dev/当前部署下各服务共用 edu_db，故直接读取；生产若按等保做库账号最小权限隔离，
 *       应改为 Feign 调用 edu-user 的批量用户信息接口。
 */
@Mapper
public interface StudentInfoMapper {

    @Select({"<script>",
            "SELECT id, real_name AS realName, student_no AS studentNo FROM sys_user WHERE id IN",
            "<foreach collection='ids' item='uid' open='(' separator=',' close=')'>#{uid}</foreach>",
            "</script>"})
    List<StudentBriefVO> selectByIds(@Param("ids") Collection<Long> ids);
}
