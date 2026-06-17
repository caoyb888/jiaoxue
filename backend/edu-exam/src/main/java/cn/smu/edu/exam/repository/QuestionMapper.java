package cn.smu.edu.exam.repository;

import cn.smu.edu.exam.domain.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    /** 全文检索：MATCH…AGAINST on content 字段（需 FULLTEXT INDEX ft_content） */
    @Select("SELECT * FROM question WHERE bank_id = #{bankId} AND is_deleted = 0 " +
            "AND MATCH(content) AGAINST(#{keyword} IN BOOLEAN MODE) " +
            "ORDER BY created_at DESC")
    IPage<Question> searchByKeyword(IPage<Question> page,
                                    @Param("bankId") Long bankId,
                                    @Param("keyword") String keyword);
}
