package cn.smu.edu.interaction.repository;

import cn.smu.edu.interaction.domain.entity.SlideFeedback;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SlideFeedbackMapper extends BaseMapper<SlideFeedback> {

    @Select("SELECT slide_page, COUNT(*) AS count FROM slide_feedback " +
            "WHERE lesson_id = #{lessonId} GROUP BY slide_page ORDER BY count DESC")
    List<Map<String, Object>> countByPage(@Param("lessonId") Long lessonId);
}
