package cn.smu.edu.notify.repository;

import cn.smu.edu.notify.domain.vo.NoticeItemVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 通知接收端查询（跨表：notice / notice_read / sys_user / class_student）。
 *
 * <p>可见性：已发布(status=2) 且当前用户命中通知范围——SCHOOL 全员 / DEPT 同院系 /
 * CLASS 本人所选班级；并按 target_roles 过滤 user_type。
 */
@Mapper
public interface NoticeQueryMapper {

    /** 通用可见性 WHERE（用户维度子查询，避免依赖网关透传的 dept/role）。 */
    String VISIBLE_WHERE = """
            n.status = 2
            AND ( n.scope = 'SCHOOL'
               OR (n.scope = 'DEPT' AND n.dept_id = (SELECT u.dept_id FROM sys_user u WHERE u.id = #{userId}))
               OR (n.scope = 'CLASS' AND n.class_id IN
                    (SELECT cs.class_id FROM class_student cs WHERE cs.student_id = #{userId} AND cs.status = 1)) )
            AND ( n.target_roles IS NULL OR n.target_roles = 'ALL'
               OR (n.target_roles = 'TEACHER' AND (SELECT u.user_type FROM sys_user u WHERE u.id = #{userId}) = 2)
               OR (n.target_roles = 'STUDENT' AND (SELECT u.user_type FROM sys_user u WHERE u.id = #{userId}) = 1) )
            """;

    /** 我可见的通知（按发布时间倒序），read 标记来自 notice_read；onlyUnread 时仅未读。 */
    @Select("""
            <script>
            SELECT n.id, n.title, n.content, n.sender_name, n.scope, n.published_at,
                   (r.id IS NOT NULL) AS `read`
            FROM notice n
            LEFT JOIN notice_read r ON r.notice_id = n.id AND r.user_id = #{userId}
            WHERE """ + VISIBLE_WHERE + """
            <if test="onlyUnread"> AND r.id IS NULL </if>
            ORDER BY n.published_at DESC
            LIMIT #{limit}
            </script>
            """)
    List<NoticeItemVO> selectMyNotices(@Param("userId") Long userId,
                                       @Param("onlyUnread") boolean onlyUnread,
                                       @Param("limit") int limit);

    /** 我的未读通知数。 */
    @Select("""
            <script>
            SELECT COUNT(*)
            FROM notice n
            LEFT JOIN notice_read r ON r.notice_id = n.id AND r.user_id = #{userId}
            WHERE """ + VISIBLE_WHERE + """
            AND r.id IS NULL
            </script>
            """)
    long countUnread(@Param("userId") Long userId);

    /** 标记已读（幂等，重复读返回 0）。 */
    @Insert("INSERT IGNORE INTO notice_read (notice_id, user_id) VALUES (#{noticeId}, #{userId})")
    int insertReadIgnore(@Param("noticeId") Long noticeId, @Param("userId") Long userId);

    /** 已读人数 +1（首次已读时调用）。 */
    @Update("UPDATE notice SET read_count = read_count + 1 WHERE id = #{noticeId}")
    int incrementReadCount(@Param("noticeId") Long noticeId);
}
