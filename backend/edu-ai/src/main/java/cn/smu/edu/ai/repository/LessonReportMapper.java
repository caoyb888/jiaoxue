package cn.smu.edu.ai.repository;

import cn.smu.edu.ai.domain.entity.LessonReport;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LessonReportMapper extends BaseMapper<LessonReport> {

    LessonReport selectByLessonId(@Param("lessonId") Long lessonId);

    int updateAiContent(@Param("lessonId") Long lessonId,
                        @Param("aiSummary") String aiSummary,
                        @Param("aiMindmapJson") String aiMindmapJson,
                        @Param("genStatus") int genStatus);
}
