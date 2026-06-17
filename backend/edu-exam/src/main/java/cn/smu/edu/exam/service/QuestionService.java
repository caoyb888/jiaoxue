package cn.smu.edu.exam.service;

import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.exam.domain.dto.QuestionCreateDTO;
import cn.smu.edu.exam.domain.dto.QuestionQueryDTO;
import cn.smu.edu.exam.domain.dto.QuestionUpdateDTO;
import cn.smu.edu.exam.domain.vo.QuestionVO;

public interface QuestionService {

    QuestionVO create(QuestionCreateDTO dto, Long creatorId, Long teacherId, Long deptId);

    QuestionVO update(Long questionId, QuestionUpdateDTO dto, Long teacherId, Long deptId);

    void delete(Long questionId, Long teacherId, Long deptId);

    QuestionVO getById(Long questionId, Long teacherId, Long deptId);

    PageResult<QuestionVO> list(QuestionQueryDTO query, Long teacherId, Long deptId);
}
