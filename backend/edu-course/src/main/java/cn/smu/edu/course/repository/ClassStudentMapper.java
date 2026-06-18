package cn.smu.edu.course.repository;

import cn.smu.edu.course.domain.entity.ClassStudent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ClassStudentMapper extends BaseMapper<ClassStudent> {

    int countByClassId(@Param("classId") Long classId);
}
