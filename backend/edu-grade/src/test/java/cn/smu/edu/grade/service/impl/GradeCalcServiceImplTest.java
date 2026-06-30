package cn.smu.edu.grade.service.impl;

import cn.smu.edu.grade.domain.entity.GradeRule;
import cn.smu.edu.grade.domain.entity.StudentGrade;
import cn.smu.edu.grade.repository.GradeRuleMapper;
import cn.smu.edu.grade.repository.StudentGradeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeCalcServiceImplTest {

    @Mock
    GradeRuleMapper gradeRuleMapper;
    @Mock
    StudentGradeMapper studentGradeMapper;

    private GradeCalcServiceImpl service() {
        return new GradeCalcServiceImpl(gradeRuleMapper, studentGradeMapper);
    }

    private GradeRule rule(int gradeType, String weight) {
        GradeRule r = new GradeRule();
        r.setGradeType(gradeType);
        r.setWeight(new BigDecimal(weight));
        return r;
    }

    private StudentGrade grade(long id, String attend, String quiz, String inter, String exam) {
        StudentGrade g = new StudentGrade();
        g.setId(id);
        g.setClassId(100L);
        g.setStudentId(id);
        g.setAttendScore(new BigDecimal(attend));
        g.setQuizScore(new BigDecimal(quiz));
        g.setInteractionScore(new BigDecimal(inter));
        g.setExamScore(new BigDecimal(exam));
        return g;
    }

    @Test
    void calculateClass_shouldApplyRuleWeightsAndSetStatus() {
        // 出勤20 + 平时30 + 期末50 = 100 → 归一 0.2/0.3/0/0.5
        when(gradeRuleMapper.selectByClassId(100L)).thenReturn(List.of(
                rule(5, "20"), rule(2, "30"), rule(1, "50")));
        // attend 100, quiz 80, interaction 0, exam 60 → 100*.2+80*.3+60*.5 = 20+24+30 = 74
        when(studentGradeMapper.selectByClassId(100L)).thenReturn(List.of(grade(1, "100", "80", "0", "60")));

        int count = service().calculateClass(100L);

        assertThat(count).isEqualTo(1);
        ArgumentCaptor<StudentGrade> cap = ArgumentCaptor.forClass(StudentGrade.class);
        verify(studentGradeMapper).updateById(cap.capture());
        assertThat(cap.getValue().getTotalScore()).isEqualByComparingTo("74.00");
        assertThat(cap.getValue().getCalcStatus()).isEqualTo(1);
    }

    @Test
    void calculateClass_noRules_shouldUseDefaultWeights() {
        // 默认 0.1/0.2/0.2/0.5；attend 90, quiz 80, inter 70, exam 60
        // = 90*.1+80*.2+70*.2+60*.5 = 9+16+14+30 = 69
        when(gradeRuleMapper.selectByClassId(100L)).thenReturn(List.of());
        when(studentGradeMapper.selectByClassId(100L)).thenReturn(List.of(grade(1, "90", "80", "70", "60")));

        service().calculateClass(100L);

        ArgumentCaptor<StudentGrade> cap = ArgumentCaptor.forClass(StudentGrade.class);
        verify(studentGradeMapper).updateById(cap.capture());
        assertThat(cap.getValue().getTotalScore()).isEqualByComparingTo("69.00");
    }

    @Test
    void resolveWeights_shouldNormalizeToSumOne() {
        GradeCalcServiceImpl.Weights w = service().resolveWeights(List.of(
                rule(5, "10"), rule(2, "20"), rule(6, "20"), rule(1, "50")));
        assertThat(w.attend()).isEqualByComparingTo("0.1");
        assertThat(w.quiz()).isEqualByComparingTo("0.2");
        assertThat(w.interaction()).isEqualByComparingTo("0.2");
        assertThat(w.exam()).isEqualByComparingTo("0.5");
    }

    @Test
    void calculatePending_shouldRecalcEachPendingClass() {
        when(studentGradeMapper.selectPendingClassIds()).thenReturn(List.of(100L, 200L));
        when(gradeRuleMapper.selectByClassId(any(Long.class))).thenReturn(List.of());
        when(studentGradeMapper.selectByClassId(100L)).thenReturn(List.of(grade(1, "100", "100", "100", "100")));
        when(studentGradeMapper.selectByClassId(200L)).thenReturn(List.of());

        int total = service().calculatePending();

        assertThat(total).isEqualTo(1); // 100班1人，200班0人
        verify(studentGradeMapper).selectByClassId(100L);
        verify(studentGradeMapper).selectByClassId(200L);
    }
}
