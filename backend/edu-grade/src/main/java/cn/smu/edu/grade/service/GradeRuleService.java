package cn.smu.edu.grade.service;

import cn.smu.edu.grade.domain.dto.GradeRuleCreateDTO;
import cn.smu.edu.grade.domain.dto.GradeRuleUpdateDTO;
import cn.smu.edu.grade.domain.vo.GradeRuleListVO;
import cn.smu.edu.grade.domain.vo.GradeRuleVO;

public interface GradeRuleService {

    /**
     * 新增成绩规则。
     * <p>规则：新增后该班级所有规则权重之和不得超过 100，否则抛 BizException。
     */
    GradeRuleVO create(Long teacherId, GradeRuleCreateDTO dto);

    /**
     * 更新成绩规则（仅允许规则所属教师操作）。
     * <p>更新后权重合计不得超过 100。
     */
    GradeRuleVO update(Long ruleId, Long teacherId, GradeRuleUpdateDTO dto);

    /** 删除成绩规则（逻辑删除） */
    void delete(Long ruleId, Long teacherId);

    /** 获取班级所有规则及权重合计 */
    GradeRuleListVO listByClass(Long classId);
}
