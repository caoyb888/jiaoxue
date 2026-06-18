package cn.smu.edu.course.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.course.domain.dto.CourseCreateDTO;
import cn.smu.edu.course.domain.dto.CourseQueryDTO;
import cn.smu.edu.course.domain.entity.Course;
import cn.smu.edu.course.domain.vo.CourseListItemVO;
import cn.smu.edu.course.repository.CourseMapper;
import cn.smu.edu.course.service.CourseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;

    @Override
    public PageResult<CourseListItemVO> listCourses(CourseQueryDTO query) {
        Page<CourseListItemVO> page = new Page<>(query.getPage(), query.getSize());
        IPage<CourseListItemVO> result = courseMapper.selectCoursePage(
                page,
                query.getSemester(),
                query.getDeptId(),
                query.getKeyword());
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCourse(CourseCreateDTO dto) {
        long count = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                .eq(Course::getCourseCode, dto.getCourseCode())
                .eq(Course::getIsDeleted, 0));
        if (count > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "课程编码已存在: " + dto.getCourseCode());
        }

        Course course = new Course();
        course.setCourseCode(dto.getCourseCode());
        course.setCourseName(dto.getCourseName());
        course.setDeptId(dto.getDeptId());
        course.setCredit(dto.getCredit());
        course.setCourseType(dto.getCourseType() != null ? dto.getCourseType() : 1);
        course.setSemester(dto.getSemester());
        course.setDescription(dto.getDescription());
        courseMapper.insert(course);

        log.info("课程创建成功: courseId={}, courseCode={}", course.getId(), course.getCourseCode());
        return course.getId();
    }
}
