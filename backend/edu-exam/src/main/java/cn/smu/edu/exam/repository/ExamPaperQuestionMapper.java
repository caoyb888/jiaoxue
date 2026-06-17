package cn.smu.edu.exam.repository;

import cn.smu.edu.exam.domain.entity.ExamPaperQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ExamPaperQuestionMapper extends BaseMapper<ExamPaperQuestion> {

    @Select("SELECT * FROM exam_paper_question WHERE paper_id = #{paperId} ORDER BY paper_group, sort_order")
    List<ExamPaperQuestion> selectByPaperId(@Param("paperId") Long paperId);

    @Select("SELECT * FROM exam_paper_question WHERE paper_id = #{paperId} AND paper_group = #{paperGroup} ORDER BY sort_order")
    List<ExamPaperQuestion> selectByPaperGroup(@Param("paperId") Long paperId, @Param("paperGroup") String paperGroup);

    @Delete("DELETE FROM exam_paper_question WHERE paper_id = #{paperId} AND question_id = #{questionId} AND paper_group = #{paperGroup}")
    int deleteByPaperAndQuestion(@Param("paperId") Long paperId,
                                 @Param("questionId") Long questionId,
                                 @Param("paperGroup") String paperGroup);

    @Delete("DELETE FROM exam_paper_question WHERE paper_id = #{paperId} AND paper_group = #{paperGroup}")
    int deleteByPaperGroup(@Param("paperId") Long paperId, @Param("paperGroup") String paperGroup);

    @Delete("DELETE FROM exam_paper_question WHERE paper_id = #{paperId}")
    int deleteAllByPaperId(@Param("paperId") Long paperId);

    @Select("SELECT COALESCE(SUM(score), 0) FROM exam_paper_question WHERE paper_id = #{paperId}")
    BigDecimal sumScoreByPaperId(@Param("paperId") Long paperId);

    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM exam_paper_question WHERE paper_id = #{paperId} AND paper_group = #{paperGroup}")
    int maxSortOrder(@Param("paperId") Long paperId, @Param("paperGroup") String paperGroup);

    @Select("SELECT COUNT(*) FROM exam_paper_question WHERE paper_id = #{paperId} AND question_id = #{questionId} AND paper_group = #{paperGroup}")
    int existsRelation(@Param("paperId") Long paperId,
                       @Param("questionId") Long questionId,
                       @Param("paperGroup") String paperGroup);
}
