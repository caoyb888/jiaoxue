package cn.smu.edu.grade.export;

import cn.smu.edu.grade.domain.entity.StudentGrade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GradeExportTemplateTest {

    private StudentGrade sample() {
        return StudentGrade.builder()
                .studentId(2021001L)
                .attendScore(new BigDecimal("10.00"))
                .quizScore(new BigDecimal("15.00"))
                .interactionScore(new BigDecimal("5.00"))
                .examScore(new BigDecimal("60.00"))
                .totalScore(new BigDecimal("90.00"))
                .offlineScore(new BigDecimal("88.00"))
                .build();
    }

    @Test
    void zhengfang_shouldHaveFourColumnsAndRegularSum() {
        ZhengfangGradeTemplate t = new ZhengfangGradeTemplate();
        assertThat(t.format()).isEqualTo("zhengfang");
        assertThat(t.headers()).containsExactly("学号", "平时成绩", "期末成绩", "总评成绩");

        List<Object> row = t.row(sample());
        assertThat(row.get(0)).isEqualTo(2021001L);
        assertThat(row.get(1)).isEqualTo(new BigDecimal("30.00"));   // 10+15+5 平时
        assertThat(row.get(2)).isEqualTo(new BigDecimal("60.00"));   // 期末
        assertThat(row.get(3)).isEqualTo(new BigDecimal("90.00"));   // 总评
    }

    @Test
    void qiangzhi_shouldHaveFiveColumnsWithRemark() {
        QiangzhiGradeTemplate t = new QiangzhiGradeTemplate();
        assertThat(t.format()).isEqualTo("qiangzhi");
        assertThat(t.headers()).containsExactly("学生学号", "平时分", "考试分", "综合成绩", "备注");

        List<Object> row = t.row(sample());
        assertThat(row).hasSize(5);
        assertThat(row.get(4)).isEqualTo("含线下成绩");                // offlineScore 非空
    }

    @Test
    void regularScore_shouldTreatNullDimensionsAsZero() {
        ZhengfangGradeTemplate t = new ZhengfangGradeTemplate();
        StudentGrade g = StudentGrade.builder().studentId(1L)
                .attendScore(new BigDecimal("8.00")).build(); // quiz/interaction 为 null
        assertThat(t.row(g).get(1)).isEqualTo(new BigDecimal("8.00"));
    }

    @Test
    void qiangzhi_remarkShouldBeEmptyWhenNoOffline() {
        QiangzhiGradeTemplate t = new QiangzhiGradeTemplate();
        StudentGrade g = StudentGrade.builder().studentId(1L).build(); // offlineScore null
        assertThat(t.row(g).get(4)).isEqualTo("");
    }
}
