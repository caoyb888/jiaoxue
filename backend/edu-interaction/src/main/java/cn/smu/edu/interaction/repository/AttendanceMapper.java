package cn.smu.edu.interaction.repository;

import cn.smu.edu.interaction.domain.entity.Attendance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AttendanceMapper extends BaseMapper<Attendance> {

    @Select("SELECT student_id FROM attendance WHERE lesson_id = #{lessonId} AND status != 0")
    List<Long> selectAttendedStudentIds(@Param("lessonId") Long lessonId);

    @Select("SELECT COUNT(*) FROM attendance WHERE lesson_id = #{lessonId} AND status != 0")
    int countAttended(@Param("lessonId") Long lessonId);
}
