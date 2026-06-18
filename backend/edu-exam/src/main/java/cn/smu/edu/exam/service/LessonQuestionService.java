package cn.smu.edu.exam.service;

import cn.smu.edu.exam.domain.dto.PublishLessonQuestionDTO;
import cn.smu.edu.exam.domain.vo.LessonQuestionVO;

import java.util.List;

public interface LessonQuestionService {

    /** 教师向课堂发布题目（自动关闭当前进行中的题目） */
    LessonQuestionVO publish(Long lessonId, Long teacherId, PublishLessonQuestionDTO dto);

    /** 教师手动关闭课堂当前题目 */
    void close(Long lessonQuestionId, Long teacherId);

    /** 获取课堂当前进行中的题目（教师/学生均可调用） */
    LessonQuestionVO getCurrent(Long lessonId);

    /** 获取本节课发布过的所有题目历史 */
    List<LessonQuestionVO> getHistory(Long lessonId);
}
