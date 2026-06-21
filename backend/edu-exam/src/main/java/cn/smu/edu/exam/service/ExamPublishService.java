package cn.smu.edu.exam.service;

import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.exam.domain.dto.*;
import cn.smu.edu.exam.domain.vo.ExamPublishStudentVO;
import cn.smu.edu.exam.domain.vo.ExamPublishVO;
import cn.smu.edu.exam.domain.vo.StudentExamListVO;

import java.util.List;

public interface ExamPublishService {

    ExamPublishVO publish(ExamPublishCreateDTO dto, Long teacherId);

    ExamPublishVO update(Long publishId, ExamPublishUpdateDTO dto, Long teacherId);

    /** 取消发布（逻辑软删除 + status=3） */
    void cancel(Long publishId, Long teacherId);

    ExamPublishVO getById(Long publishId, Long teacherId);

    PageResult<ExamPublishVO> listByTeacher(ExamPublishQueryDTO query, Long teacherId);

    /** 学生视角：校验密码（若有），返回试卷题目；answer 按 answerShowAt 控制可见性 */
    ExamPublishStudentVO getStudentView(Long publishId, Long studentId, String password);

    /** 验证考试密码（独立接口，返回临时令牌或 true/false） */
    boolean verifyPassword(Long publishId, String password);

    /** 学生端：查询指定班级的考试列表（含是否已进入/已交卷状态） */
    List<StudentExamListVO> listForStudent(Long classId, Long studentId);

    /** XXL-Job：批量维护 exam_publish.status 冗余字段（0/1/2状态同步） */
    void syncExamStatus();
}
