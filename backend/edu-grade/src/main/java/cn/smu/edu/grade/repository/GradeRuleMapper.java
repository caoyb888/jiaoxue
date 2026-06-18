package cn.smu.edu.grade.repository;

import cn.smu.edu.grade.domain.entity.GradeRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface GradeRuleMapper extends BaseMapper<GradeRule> {

    @Select("SELECT * FROM grade_rule WHERE class_id = #{classId} AND is_deleted = 0 ORDER BY grade_type, created_at")
    List<GradeRule> selectByClassId(@Param("classId") Long classId);

    @Select("SELECT COALESCE(SUM(weight), 0) FROM grade_rule WHERE class_id = #{classId} AND is_deleted = 0")
    BigDecimal sumWeightByClassId(@Param("classId") Long classId);

    /** 排除指定 ID 的权重合计（用于 update 时重算） */
    @Select("SELECT COALESCE(SUM(weight), 0) FROM grade_rule WHERE class_id = #{classId} AND id != #{excludeId} AND is_deleted = 0")
    BigDecimal sumWeightByClassIdExclude(@Param("classId") Long classId, @Param("excludeId") Long excludeId);
}
