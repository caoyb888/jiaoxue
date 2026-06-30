package cn.smu.edu.notify.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 通知目标用户解析（跨域只读查询：sys_user / class_student / user_wechat）。
 */
@Mapper
public interface NoticeTargetMapper {

    /**
     * 全校（deptId=null）或指定院系的活跃用户 ID，可按 userType 过滤角色。
     *
     * @param deptId   院系ID，null=全校
     * @param userType 用户类型 1学生/2教师，null=不限
     */
    @Select("""
            <script>
            SELECT id FROM sys_user
            WHERE status = 1 AND is_deleted = 0
            <if test="deptId != null"> AND dept_id = #{deptId} </if>
            <if test="userType != null"> AND user_type = #{userType} </if>
            </script>
            """)
    List<Long> selectUserIds(@Param("deptId") Long deptId, @Param("userType") Integer userType);

    /** 指定教学班的正常选课学生 ID。 */
    @Select("SELECT student_id FROM class_student WHERE class_id = #{classId} AND status = 1")
    List<Long> selectClassStudentIds(@Param("classId") Long classId);

    /** 批量查用户的微信 openId（仅已绑定微信者；用于订阅消息推送）。 */
    @Select("""
            <script>
            SELECT open_id FROM user_wechat
            WHERE user_id IN
            <foreach collection="userIds" item="uid" open="(" separator="," close=")">#{uid}</foreach>
            AND open_id IS NOT NULL
            </script>
            """)
    List<String> selectOpenIdsByUserIds(@Param("userIds") List<Long> userIds);
}
