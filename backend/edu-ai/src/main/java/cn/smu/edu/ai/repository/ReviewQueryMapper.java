package cn.smu.edu.ai.repository;

import cn.smu.edu.ai.domain.dto.ReviewItemDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 跨表只读查询：拉取某次试卷发布下、尚未批改的主观题作答（question.type=5）。
 * edu-ai 与业务库 edu_db 同库，直接只读查询；写回由 S6-03 通过事件交回 edu-exam。
 */
@Mapper
public interface ReviewQueryMapper {

    @Select("""
            SELECT sa.id              AS answerId,
                   sa.question_id     AS questionId,
                   sa.student_id      AS studentId,
                   q.content          AS questionContent,
                   q.answer           AS referenceAnswer,
                   q.review_rule      AS reviewRule,
                   sa.answer_content  AS studentAnswer,
                   q.score            AS maxScore
            FROM student_answer sa
            JOIN question q ON q.id = sa.question_id AND q.is_deleted = 0
            WHERE sa.publish_id = #{publishId}
              AND sa.is_deleted = 0
              AND sa.review_status = 0
              AND q.type = 5
            """)
    List<ReviewItemDTO> selectPendingSubjective(@Param("publishId") Long publishId);
}
