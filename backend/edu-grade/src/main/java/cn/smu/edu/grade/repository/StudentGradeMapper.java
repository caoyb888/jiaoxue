package cn.smu.edu.grade.repository;

import cn.smu.edu.grade.domain.entity.StudentGrade;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StudentGradeMapper extends BaseMapper<StudentGrade> {

    /** 按教学班查全部已计算成绩，按学号（student_id）升序，供回传导出。 */
    @Select("""
            SELECT id, class_id, student_id, total_score, attend_score, quiz_score,
                   interaction_score, exam_score, offline_score, calc_status, updated_at
            FROM student_grade
            WHERE class_id = #{classId}
            ORDER BY student_id
            """)
    List<StudentGrade> selectByClassId(@Param("classId") Long classId);
}
