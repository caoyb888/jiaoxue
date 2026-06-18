package cn.smu.edu.course.repository;

import cn.smu.edu.course.domain.entity.Lesson;
import cn.smu.edu.course.domain.vo.LessonDetailVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LessonMapper extends BaseMapper<Lesson> {

    IPage<LessonDetailVO> selectLessonPage(
            Page<LessonDetailVO> page,
            @Param("classId") Long classId,
            @Param("status") Integer status);

    LessonDetailVO selectLessonDetail(@Param("lessonId") Long lessonId);
}
