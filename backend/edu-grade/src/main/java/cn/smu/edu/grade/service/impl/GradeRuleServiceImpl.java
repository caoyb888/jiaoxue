package cn.smu.edu.grade.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.grade.domain.dto.GradeRuleCreateDTO;
import cn.smu.edu.grade.domain.dto.GradeRuleUpdateDTO;
import cn.smu.edu.grade.domain.entity.GradeRule;
import cn.smu.edu.grade.domain.vo.GradeRuleListVO;
import cn.smu.edu.grade.domain.vo.GradeRuleVO;
import cn.smu.edu.grade.repository.GradeRuleMapper;
import cn.smu.edu.grade.service.GradeRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GradeRuleServiceImpl implements GradeRuleService {

    private static final BigDecimal MAX_WEIGHT = new BigDecimal("100.00");

    private static final Map<Integer, String> TYPE_NAMES = Map.of(
            1, "期末考试",
            2, "平时作业",
            3, "实验报告",
            4, "项目实践",
            5, "出勤",
            6, "其他"
    );

    private final GradeRuleMapper gradeRuleMapper;

    @Override
    @Transactional
    public GradeRuleVO create(Long teacherId, GradeRuleCreateDTO dto) {
        BigDecimal existingWeight = gradeRuleMapper.sumWeightByClassId(dto.getClassId());
        BigDecimal newTotal = existingWeight.add(dto.getWeight());
        if (newTotal.compareTo(MAX_WEIGHT) > 0) {
            throw new BizException(ErrorCode.GRADE_RULE_WEIGHT_ERROR);
        }

        GradeRule rule = new GradeRule();
        rule.setClassId(dto.getClassId());
        rule.setTeacherId(teacherId);
        rule.setRuleName(dto.getRuleName());
        rule.setGradeType(dto.getGradeType());
        rule.setWeight(dto.getWeight());
        rule.setDescription(dto.getDescription());
        gradeRuleMapper.insert(rule);

        log.info("创建成绩规则: classId={}, ruleName={}, weight={}, newTotal={}",
                dto.getClassId(), dto.getRuleName(), dto.getWeight(), newTotal);
        return toVO(rule);
    }

    @Override
    @Transactional
    public GradeRuleVO update(Long ruleId, Long teacherId, GradeRuleUpdateDTO dto) {
        GradeRule rule = gradeRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (!rule.getTeacherId().equals(teacherId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        // 排除当前规则后，加上新权重，不得超过 100
        BigDecimal otherWeight = gradeRuleMapper.sumWeightByClassIdExclude(rule.getClassId(), ruleId);
        BigDecimal newTotal = otherWeight.add(dto.getWeight());
        if (newTotal.compareTo(MAX_WEIGHT) > 0) {
            throw new BizException(ErrorCode.GRADE_RULE_WEIGHT_ERROR);
        }

        rule.setRuleName(dto.getRuleName());
        rule.setGradeType(dto.getGradeType());
        rule.setWeight(dto.getWeight());
        rule.setDescription(dto.getDescription());
        gradeRuleMapper.updateById(rule);

        log.info("更新成绩规则: id={}, weight={}, newTotal={}", ruleId, dto.getWeight(), newTotal);
        return toVO(rule);
    }

    @Override
    @Transactional
    public void delete(Long ruleId, Long teacherId) {
        GradeRule rule = gradeRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (!rule.getTeacherId().equals(teacherId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        gradeRuleMapper.deleteById(ruleId);
        log.info("删除成绩规则: id={}", ruleId);
    }

    @Override
    public GradeRuleListVO listByClass(Long classId) {
        List<GradeRule> rules = gradeRuleMapper.selectByClassId(classId);
        BigDecimal totalWeight = gradeRuleMapper.sumWeightByClassId(classId);

        GradeRuleListVO vo = new GradeRuleListVO();
        vo.setClassId(classId);
        vo.setRules(rules.stream().map(this::toVO).collect(Collectors.toList()));
        vo.setTotalWeight(totalWeight);
        vo.setWeightComplete(totalWeight.compareTo(MAX_WEIGHT) == 0);
        return vo;
    }

    // ── 类型名称映射 ─────────────────────────────────────────────────────────

    private GradeRuleVO toVO(GradeRule rule) {
        GradeRuleVO vo = new GradeRuleVO();
        vo.setId(rule.getId());
        vo.setClassId(rule.getClassId());
        vo.setRuleName(rule.getRuleName());
        vo.setGradeType(rule.getGradeType());
        vo.setGradeTypeName(TYPE_NAMES.getOrDefault(rule.getGradeType(), "未知"));
        vo.setWeight(rule.getWeight());
        vo.setDescription(rule.getDescription());
        vo.setCreatedAt(rule.getCreatedAt());
        vo.setUpdatedAt(rule.getUpdatedAt());
        return vo;
    }
}
