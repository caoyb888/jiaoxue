package cn.smu.edu.exam.repository;

import cn.smu.edu.exam.domain.entity.StudentAnswer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface StudentAnswerMapper extends BaseMapper<StudentAnswer> {

    @Select("SELECT * FROM student_answer WHERE publish_id = #{publishId} AND student_id = #{studentId} AND is_deleted = 0 ORDER BY question_id")
    List<StudentAnswer> selectByPublishAndStudent(@Param("publishId") Long publishId,
                                                  @Param("studentId") Long studentId);

    @Select("SELECT COALESCE(SUM(score), 0) FROM student_answer WHERE publish_id = #{publishId} AND student_id = #{studentId} AND score IS NOT NULL AND is_deleted = 0")
    BigDecimal sumScoreByPublishAndStudent(@Param("publishId") Long publishId,
                                           @Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM student_answer WHERE publish_id = #{publishId} AND student_id = #{studentId} AND review_status > 0 AND is_deleted = 0")
    int countGraded(@Param("publishId") Long publishId, @Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM student_answer WHERE publish_id = #{publishId} AND student_id = #{studentId} AND is_correct = 1 AND is_deleted = 0")
    int countCorrect(@Param("publishId") Long publishId, @Param("studentId") Long studentId);

    @Select("SELECT COUNT(*) FROM student_answer WHERE publish_id = #{publishId} AND question_id = #{questionId} AND student_id = #{studentId} AND is_deleted = 0")
    int existsAnswer(@Param("publishId") Long publishId,
                     @Param("questionId") Long questionId,
                     @Param("studentId") Long studentId);
}
