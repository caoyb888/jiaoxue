package cn.smu.edu.ai.repository;

import cn.smu.edu.ai.domain.entity.GeneratedQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI 出题批量写入 question 表（edu-ai 与 edu_db 同库）。
 * 批量单条 INSERT，避免循环单插（CLAUDE.md 5.4）；useGeneratedKeys 回填各题 id。
 */
@Mapper
public interface QuestionWriteMapper {

    @Insert("""
            <script>
            INSERT INTO question (bank_id, type, content, answer, analysis, score, difficulty, creator_id)
            VALUES
            <foreach collection="list" item="q" separator=",">
                (#{q.bankId}, #{q.type}, #{q.content}, #{q.answer}, #{q.analysis}, #{q.score}, #{q.difficulty}, #{q.creatorId})
            </foreach>
            </script>
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertBatch(@Param("list") List<GeneratedQuestion> list);
}
