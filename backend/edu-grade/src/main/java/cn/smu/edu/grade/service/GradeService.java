package cn.smu.edu.grade.service;

import cn.smu.edu.grade.domain.vo.OfflineImportResultVO;
import cn.smu.edu.grade.domain.vo.StudentGradeVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 成绩查询与线下成绩导入（S8-09）。
 */
public interface GradeService {

    /** 查询教学班成绩列表（各维度 + 总分 + 线下成绩），按学号升序。 */
    List<StudentGradeVO> listClassGrades(Long classId);

    /**
     * 导入线下成绩 xlsx（列：学号 | 线下成绩），写回 {@code student_grade.offline_score}。
     * 行内逐行容错，返回成功/失败明细。
     */
    OfflineImportResultVO importOffline(Long classId, MultipartFile file);
}
