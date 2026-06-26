package cn.smu.edu.grade.export;

import cn.smu.edu.grade.domain.entity.StudentGrade;

import java.util.List;

/**
 * 成绩回传 Excel 模板（S7-11）。
 *
 * <p>屏蔽正方/强智回传模板差异：表头列名/顺序、各列取值不同。导出服务只依赖本接口，
 * 新增厂商只需加一个实现。
 */
public interface GradeExportTemplate {

    /** 格式标识：zhengfang / qiangzhi。 */
    String format();

    /** 工作表名。 */
    String sheetName();

    /** 表头列（顺序即列顺序）。 */
    List<String> headers();

    /** 单个学生成绩 → 一行单元格值（顺序与 {@link #headers()} 对齐，可含 null=空单元格）。 */
    List<Object> row(StudentGrade grade);
}
