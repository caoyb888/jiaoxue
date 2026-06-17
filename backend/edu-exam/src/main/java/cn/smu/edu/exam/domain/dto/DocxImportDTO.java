package cn.smu.edu.exam.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/** docx 批量导题请求参数（题库 ID 通过 path 传递，文件通过 multipart 传递） */
@Data
public class DocxImportDTO {

    /** 导入题目的默认单题分值（试卷组卷时可覆盖） */
    @Positive
    private BigDecimal defaultScore = new BigDecimal("2.00");
}
