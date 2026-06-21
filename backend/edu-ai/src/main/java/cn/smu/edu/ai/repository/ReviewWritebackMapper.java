package cn.smu.edu.ai.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 批改结果写回 student_answer（review_status=1 自动批改完成）。
 * edu-ai 与 edu_db 同库，直接更新；仅更新解析成功的题目。
 */
@Mapper
public interface ReviewWritebackMapper {

    @Update("""
            UPDATE student_answer
               SET score = #{score},
                   comment = #{comment},
                   review_status = 1
             WHERE id = #{answerId}
               AND is_deleted = 0
               AND review_status = 0
            """)
    int writeBack(@Param("answerId") Long answerId,
                  @Param("score") BigDecimal score,
                  @Param("comment") String comment);
}
