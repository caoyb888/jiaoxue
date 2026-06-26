package cn.smu.edu.grade.export;

import cn.smu.edu.grade.domain.entity.StudentGrade;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 强智成绩回传模板：学生学号 | 平时分 | 考试分 | 综合成绩 | 备注（与正方列名/列数不同）。
 */
@Component
public class QiangzhiGradeTemplate extends AbstractGradeExportTemplate {

    @Override
    public String format() {
        return "qiangzhi";
    }

    @Override
    public String sheetName() {
        return "成绩导入";
    }

    @Override
    public List<String> headers() {
        return List.of("学生学号", "平时分", "考试分", "综合成绩", "备注");
    }

    @Override
    public List<Object> row(StudentGrade g) {
        return Arrays.asList(
                g.getStudentId(),
                regularScore(g),
                g.getExamScore(),
                g.getTotalScore(),
                g.getOfflineScore() != null ? "含线下成绩" : "");
    }
}
