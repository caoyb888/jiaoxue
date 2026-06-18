package cn.smu.edu.course.repository;

import cn.smu.edu.course.domain.entity.LessonSchedule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LessonScheduleMapper extends BaseMapper<LessonSchedule> {
}
