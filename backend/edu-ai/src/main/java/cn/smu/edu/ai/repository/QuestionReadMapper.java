package cn.smu.edu.ai.repository;

import cn.smu.edu.ai.domain.vo.GeneratedQuestionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 一键出题结果回读 question 表（edu-ai 与 edu_db 同库）。
 */
@Mapper
public interface QuestionReadMapper {

    @Select("""
            <script>
            SELECT id, type, content, answer, analysis, score, difficulty
            FROM question
            WHERE is_deleted = 0
              AND id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            ORDER BY id
            </script>
            """)
    List<GeneratedQuestionVO> selectByIds(@Param("ids") List<Long> ids);
}
