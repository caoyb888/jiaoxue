package cn.smu.edu.exam.service;

import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.exam.domain.dto.QuestionBankCreateDTO;
import cn.smu.edu.exam.domain.dto.QuestionBankQueryDTO;
import cn.smu.edu.exam.domain.dto.QuestionBankUpdateDTO;
import cn.smu.edu.exam.domain.vo.QuestionBankVO;

public interface QuestionBankService {

    QuestionBankVO create(QuestionBankCreateDTO dto, Long teacherId, Long deptId);

    QuestionBankVO update(Long bankId, QuestionBankUpdateDTO dto, Long teacherId);

    void delete(Long bankId, Long teacherId);

    QuestionBankVO getById(Long bankId, Long teacherId, Long deptId);

    /** 查询当前教师可见的题库：自己的私有库 + 同院系的公开库 */
    PageResult<QuestionBankVO> list(QuestionBankQueryDTO query, Long teacherId, Long deptId);
}
