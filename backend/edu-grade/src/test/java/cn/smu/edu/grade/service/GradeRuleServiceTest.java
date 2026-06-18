package cn.smu.edu.grade.service;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.grade.domain.dto.GradeRuleCreateDTO;
import cn.smu.edu.grade.domain.dto.GradeRuleUpdateDTO;
import cn.smu.edu.grade.domain.entity.GradeRule;
import cn.smu.edu.grade.domain.vo.GradeRuleListVO;
import cn.smu.edu.grade.domain.vo.GradeRuleVO;
import cn.smu.edu.grade.repository.GradeRuleMapper;
import cn.smu.edu.grade.service.impl.GradeRuleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradeRuleServiceTest {

    @Mock private GradeRuleMapper gradeRuleMapper;

    @InjectMocks private GradeRuleServiceImpl service;

    private static final Long CLASS_ID  = 1L;
    private static final Long TEACHER_ID = 99L;
    private static final Long RULE_ID    = 10L;

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_shouldInsert_whenTotalWeightNotExceeding100() {
        when(gradeRuleMapper.sumWeightByClassId(CLASS_ID)).thenReturn(new BigDecimal("60.00"));
        when(gradeRuleMapper.insert((GradeRule) any())).thenReturn(1);

        GradeRuleCreateDTO dto = createDTO(1, "期末考试", new BigDecimal("40.00"));
        GradeRuleVO result = service.create(TEACHER_ID, dto);

        assertThat(result.getRuleName()).isEqualTo("期末考试");
        assertThat(result.getWeight()).isEqualByComparingTo("40.00");
        assertThat(result.getGradeTypeName()).isEqualTo("期末考试");
        verify(gradeRuleMapper).insert((GradeRule) any());
    }

    @Test
    void create_shouldThrow_whenWeightExceeds100() {
        when(gradeRuleMapper.sumWeightByClassId(CLASS_ID)).thenReturn(new BigDecimal("70.00"));

        GradeRuleCreateDTO dto = createDTO(2, "平时作业", new BigDecimal("40.00")); // 70+40=110

        assertThatThrownBy(() -> service.create(TEACHER_ID, dto))
                .isInstanceOf(BizException.class);
        verify(gradeRuleMapper, never()).insert((GradeRule) any());
    }

    @Test
    void create_shouldAllow_whenWeightExactly100() {
        when(gradeRuleMapper.sumWeightByClassId(CLASS_ID)).thenReturn(new BigDecimal("60.00"));
        when(gradeRuleMapper.insert((GradeRule) any())).thenReturn(1);

        GradeRuleCreateDTO dto = createDTO(1, "期末", new BigDecimal("40.00")); // 60+40=100

        assertThatCode(() -> service.create(TEACHER_ID, dto)).doesNotThrowAnyException();
    }

    @Test
    void create_shouldInsert_whenClassHasNoRulesYet() {
        when(gradeRuleMapper.sumWeightByClassId(CLASS_ID)).thenReturn(BigDecimal.ZERO);
        when(gradeRuleMapper.insert((GradeRule) any())).thenReturn(1);

        GradeRuleCreateDTO dto = createDTO(5, "出勤", new BigDecimal("20.00"));
        GradeRuleVO vo = service.create(TEACHER_ID, dto);

        assertThat(vo.getGradeTypeName()).isEqualTo("出勤");
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_shouldSucceed_whenOtherWeightPlusNewWeightLe100() {
        GradeRule existing = gradeRule(RULE_ID, CLASS_ID, TEACHER_ID, new BigDecimal("30.00"));
        when(gradeRuleMapper.selectById(RULE_ID)).thenReturn(existing);
        when(gradeRuleMapper.sumWeightByClassIdExclude(CLASS_ID, RULE_ID)).thenReturn(new BigDecimal("50.00"));
        when(gradeRuleMapper.updateById((GradeRule) any())).thenReturn(1);

        GradeRuleUpdateDTO dto = new GradeRuleUpdateDTO();
        dto.setRuleName("期末考试修改"); dto.setGradeType(1); dto.setWeight(new BigDecimal("50.00"));
        GradeRuleVO result = service.update(RULE_ID, TEACHER_ID, dto);

        assertThat(result.getWeight()).isEqualByComparingTo("50.00");
        assertThat(result.getRuleName()).isEqualTo("期末考试修改");
    }

    @Test
    void update_shouldThrow_whenNewWeightExceeds100() {
        GradeRule existing = gradeRule(RULE_ID, CLASS_ID, TEACHER_ID, new BigDecimal("30.00"));
        when(gradeRuleMapper.selectById(RULE_ID)).thenReturn(existing);
        when(gradeRuleMapper.sumWeightByClassIdExclude(CLASS_ID, RULE_ID)).thenReturn(new BigDecimal("70.00"));

        GradeRuleUpdateDTO dto = new GradeRuleUpdateDTO();
        dto.setRuleName("期末"); dto.setGradeType(1); dto.setWeight(new BigDecimal("40.00")); // 70+40=110

        assertThatThrownBy(() -> service.update(RULE_ID, TEACHER_ID, dto))
                .isInstanceOf(BizException.class);
    }

    @Test
    void update_shouldThrow_whenNotOwner() {
        GradeRule existing = gradeRule(RULE_ID, CLASS_ID, TEACHER_ID, new BigDecimal("30.00"));
        when(gradeRuleMapper.selectById(RULE_ID)).thenReturn(existing);

        GradeRuleUpdateDTO dto = new GradeRuleUpdateDTO();
        dto.setRuleName("x"); dto.setGradeType(1); dto.setWeight(new BigDecimal("30.00"));

        assertThatThrownBy(() -> service.update(RULE_ID, 888L, dto))
                .isInstanceOf(BizException.class);
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        when(gradeRuleMapper.selectById(999L)).thenReturn(null);
        GradeRuleUpdateDTO dto = new GradeRuleUpdateDTO();
        dto.setRuleName("x"); dto.setGradeType(1); dto.setWeight(new BigDecimal("10.00"));

        assertThatThrownBy(() -> service.update(999L, TEACHER_ID, dto))
                .isInstanceOf(BizException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_shouldCallDeleteById_whenOwner() {
        GradeRule existing = gradeRule(RULE_ID, CLASS_ID, TEACHER_ID, new BigDecimal("40.00"));
        when(gradeRuleMapper.selectById(RULE_ID)).thenReturn(existing);

        service.delete(RULE_ID, TEACHER_ID);

        verify(gradeRuleMapper).deleteById((Long) RULE_ID);
    }

    @Test
    void delete_shouldThrow_whenNotOwner() {
        GradeRule existing = gradeRule(RULE_ID, CLASS_ID, TEACHER_ID, new BigDecimal("40.00"));
        when(gradeRuleMapper.selectById(RULE_ID)).thenReturn(existing);

        assertThatThrownBy(() -> service.delete(RULE_ID, 777L))
                .isInstanceOf(BizException.class);
        verify(gradeRuleMapper, never()).deleteById((Long) any());
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(gradeRuleMapper.selectById(404L)).thenReturn(null);
        assertThatThrownBy(() -> service.delete(404L, TEACHER_ID))
                .isInstanceOf(BizException.class);
    }

    // ── listByClass ───────────────────────────────────────────────────────────

    @Test
    void listByClass_shouldReturnRulesWithWeightSummary() {
        GradeRule r1 = gradeRule(1L, CLASS_ID, TEACHER_ID, new BigDecimal("60.00"));
        r1.setRuleName("期末考试"); r1.setGradeType(1);
        GradeRule r2 = gradeRule(2L, CLASS_ID, TEACHER_ID, new BigDecimal("40.00"));
        r2.setRuleName("平时作业"); r2.setGradeType(2);

        when(gradeRuleMapper.selectByClassId(CLASS_ID)).thenReturn(List.of(r1, r2));
        when(gradeRuleMapper.sumWeightByClassId(CLASS_ID)).thenReturn(new BigDecimal("100.00"));

        GradeRuleListVO result = service.listByClass(CLASS_ID);

        assertThat(result.getRules()).hasSize(2);
        assertThat(result.getTotalWeight()).isEqualByComparingTo("100.00");
        assertThat(result.getWeightComplete()).isTrue();
    }

    @Test
    void listByClass_shouldReturnWeightIncomplete_whenTotalLessThan100() {
        GradeRule r1 = gradeRule(1L, CLASS_ID, TEACHER_ID, new BigDecimal("60.00"));
        r1.setGradeType(1); r1.setRuleName("期末");

        when(gradeRuleMapper.selectByClassId(CLASS_ID)).thenReturn(List.of(r1));
        when(gradeRuleMapper.sumWeightByClassId(CLASS_ID)).thenReturn(new BigDecimal("60.00"));

        GradeRuleListVO result = service.listByClass(CLASS_ID);

        assertThat(result.getTotalWeight()).isEqualByComparingTo("60.00");
        assertThat(result.getWeightComplete()).isFalse();
    }

    @Test
    void listByClass_shouldReturnEmpty_whenNoRules() {
        when(gradeRuleMapper.selectByClassId(CLASS_ID)).thenReturn(List.of());
        when(gradeRuleMapper.sumWeightByClassId(CLASS_ID)).thenReturn(BigDecimal.ZERO);

        GradeRuleListVO result = service.listByClass(CLASS_ID);

        assertThat(result.getRules()).isEmpty();
        assertThat(result.getWeightComplete()).isFalse();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private GradeRuleCreateDTO createDTO(int type, String name, BigDecimal weight) {
        GradeRuleCreateDTO dto = new GradeRuleCreateDTO();
        dto.setClassId(CLASS_ID);
        dto.setRuleName(name);
        dto.setGradeType(type);
        dto.setWeight(weight);
        return dto;
    }

    private GradeRule gradeRule(Long id, Long classId, Long teacherId, BigDecimal weight) {
        GradeRule r = new GradeRule();
        r.setId(id);
        r.setClassId(classId);
        r.setTeacherId(teacherId);
        r.setWeight(weight);
        return r;
    }
}
