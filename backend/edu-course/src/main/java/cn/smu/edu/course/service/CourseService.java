package cn.smu.edu.course.service;

import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.course.domain.dto.CourseCreateDTO;
import cn.smu.edu.course.domain.dto.CourseQueryDTO;
import cn.smu.edu.course.domain.vo.CourseListItemVO;

public interface CourseService {

    PageResult<CourseListItemVO> listCourses(CourseQueryDTO query);

    Long createCourse(CourseCreateDTO dto);
}
