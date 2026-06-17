package cn.smu.edu.exam.repository;

import cn.smu.edu.exam.domain.entity.ExamPublish;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExamPublishMapper extends BaseMapper<ExamPublish> {

    @Select("SELECT * FROM exam_publish WHERE class_id = #{classId} AND is_deleted = 0 ORDER BY start_time DESC")
    List<ExamPublish> selectByClassId(@Param("classId") Long classId);
}
