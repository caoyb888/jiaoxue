package cn.smu.edu.exam.service;

import cn.smu.edu.exam.domain.vo.DocxImportResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public interface DocxImportService {

    /**
     * 解析 docx 模板，批量导入题目至指定题库。
     * <p>
     * 模板格式（每道题以类型标记 [xxx] 开头，空行分隔）：
     * <pre>
     * [单选题]
     * 题干内容（可多行）
     * A. 选项A
     * B. 选项B
     * 答案：A
     * 解析：...（可选）
     *
     * [判断题]
     * 题干内容
     * 答案：正确
     * </pre>
     *
     * @param bankId       目标题库 ID
     * @param file         上传的 .docx 文件
     * @param teacherId    操作教师 ID（作为题目归属）
     * @param defaultScore 题目默认分值
     */
    DocxImportResultVO importFromDocx(Long bankId, MultipartFile file,
                                      Long teacherId, BigDecimal defaultScore);
}
