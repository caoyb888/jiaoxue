package cn.smu.edu.grade.export;

import cn.smu.edu.grade.domain.entity.StudentGrade;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 正方成绩回传模板：学号 | 平时成绩 | 期末成绩 | 总评成绩。
 */
@Component
public class ZhengfangGradeTemplate extends AbstractGradeExportTemplate {

    @Override
    public String format() {
        return "zhengfang";
    }

    @Override
    public String sheetName() {
        return "成绩回传";
    }

    @Override
    public List<String> headers() {
        return List.of("学号", "平时成绩", "期末成绩", "总评成绩");
    }

    @Override
    public List<Object> row(StudentGrade g) {
        return Arrays.asList(
                g.getStudentId(),
                regularScore(g),
                g.getExamScore(),
                g.getTotalScore());
    }
}
