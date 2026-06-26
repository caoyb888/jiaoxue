package cn.smu.edu.grade.service;

/**
 * 成绩回传导出（S7-11）——按正方/强智模板生成 Excel 字节流。
 */
public interface GradeExportService {

    /**
     * 导出某教学班成绩为 xlsx 字节。
     *
     * @param classId 教学班 ID
     * @param format  导出格式 zhengfang / qiangzhi
     * @return xlsx 文件字节
     */
    byte[] exportClassGrades(Long classId, String format);
}
