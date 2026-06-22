package cn.smu.edu.ai.repository;

import cn.smu.edu.ai.domain.entity.LessonReport;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LessonReportMapper extends BaseMapper<LessonReport> {

    LessonReport selectByLessonId(@Param("lessonId") Long lessonId);

    /** 写入摘要 + 关键点 + 思维导图（SUMMARY 任务，一次性落 gen_status） */
    int updateAiContent(@Param("lessonId") Long lessonId,
                        @Param("aiSummary") String aiSummary,
                        @Param("aiKeyPoints") String aiKeyPoints,
                        @Param("aiMindmapJson") String aiMindmapJson,
                        @Param("genStatus") int genStatus);

    /** 仅更新思维导图列（MINDMAP 任务，不覆盖已有摘要/关键点） */
    int updateMindmap(@Param("lessonId") Long lessonId,
                      @Param("aiMindmapJson") String aiMindmapJson,
                      @Param("genStatus") int genStatus);

    /** 仅更新生成状态（失败时不清空已生成内容） */
    int updateGenStatus(@Param("lessonId") Long lessonId,
                        @Param("genStatus") int genStatus);

    /** 更新思维导图对学生可见性 */
    int updateMindmapVisible(@Param("lessonId") Long lessonId,
                             @Param("visible") int visible);
}
