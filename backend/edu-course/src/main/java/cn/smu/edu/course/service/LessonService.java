package cn.smu.edu.course.service;

import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.course.domain.dto.LessonQueryDTO;
import cn.smu.edu.course.domain.dto.LessonScheduleCreateDTO;
import cn.smu.edu.course.domain.dto.LessonStartDTO;
import cn.smu.edu.course.domain.vo.LessonDetailVO;
import cn.smu.edu.course.domain.vo.LessonEndVO;
import cn.smu.edu.course.domain.vo.LessonScheduleVO;
import cn.smu.edu.course.domain.vo.LessonStartVO;

public interface LessonService {

    LessonStartVO startLesson(Long teacherId, LessonStartDTO dto);

    LessonEndVO endLesson(Long lessonId, Long teacherId);

    PageResult<LessonDetailVO> listLessons(LessonQueryDTO query);

    LessonDetailVO getLessonDetail(Long lessonId);

    void updateCurrentSlide(Long lessonId, Integer slideNo, Long teacherId);

    Long createSchedule(Long teacherId, LessonScheduleCreateDTO dto);

    LessonScheduleVO getSchedule(Long scheduleId);
}
