package cn.smu.edu.exam.repository;

import cn.smu.edu.exam.domain.entity.QuestionOption;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuestionOptionMapper extends BaseMapper<QuestionOption> {

    @Select("SELECT * FROM question_option WHERE question_id = #{questionId} ORDER BY sort_order")
    List<QuestionOption> selectByQuestionId(@Param("questionId") Long questionId);

    @Delete("DELETE FROM question_option WHERE question_id = #{questionId}")
    int deleteByQuestionId(@Param("questionId") Long questionId);
}
