package cn.smu.edu.course.repository;

import cn.smu.edu.course.domain.entity.Course;
import cn.smu.edu.course.domain.vo.CourseListItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    IPage<CourseListItemVO> selectCoursePage(
            Page<CourseListItemVO> page,
            @Param("semester") String semester,
            @Param("deptId") Long deptId,
            @Param("keyword") String keyword);
}
