package cn.smu.edu.live.repository;

import cn.smu.edu.live.domain.dto.LessonLiveInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 课堂直播信息只读访问（lesson 表归 edu-course 所有，edu-live 仅读取 live_mode 等字段，不写）。
 */
@Mapper
public interface LessonLiveMapper {

    @Select("SELECT live_mode, teacher_id, status, replay_visible FROM lesson "
            + "WHERE id = #{lessonId} AND is_deleted = 0")
    LessonLiveInfo selectLiveInfo(@Param("lessonId") Long lessonId);
}
