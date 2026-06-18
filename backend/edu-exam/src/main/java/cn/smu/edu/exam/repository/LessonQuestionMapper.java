package cn.smu.edu.exam.repository;

import cn.smu.edu.exam.domain.entity.LessonQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LessonQuestionMapper extends BaseMapper<LessonQuestion> {

    @Select("SELECT * FROM lesson_question WHERE lesson_id = #{lessonId} AND status = 0 AND is_deleted = 0 ORDER BY opened_at DESC LIMIT 1")
    LessonQuestion selectCurrentByLesson(@Param("lessonId") Long lessonId);

    @Select("SELECT * FROM lesson_question WHERE lesson_id = #{lessonId} AND is_deleted = 0 ORDER BY opened_at DESC")
    List<LessonQuestion> selectByLesson(@Param("lessonId") Long lessonId);

    /** 关闭同一节课所有进行中的题目（发布新题前调用，确保只有一题进行中） */
    @Update("UPDATE lesson_question SET status = 1, closed_at = NOW() WHERE lesson_id = #{lessonId} AND status = 0 AND is_deleted = 0")
    int closeAllByLesson(@Param("lessonId") Long lessonId);
}
