package cn.smu.edu.exam.repository;

import cn.smu.edu.exam.domain.entity.LessonQuestionAnswer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LessonQuestionAnswerMapper extends BaseMapper<LessonQuestionAnswer> {

    /** 查询某学生对某道课堂题目的既有作答（用于幂等/改答） */
    @Select("SELECT * FROM lesson_question_answer WHERE lesson_question_id = #{lqId} AND student_id = #{studentId} LIMIT 1")
    LessonQuestionAnswer selectByLqAndStudent(@Param("lqId") Long lqId, @Param("studentId") Long studentId);
}
