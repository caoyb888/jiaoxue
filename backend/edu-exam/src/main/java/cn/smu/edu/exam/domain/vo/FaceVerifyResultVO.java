package cn.smu.edu.exam.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 人脸核验结果 VO。
 * C6合规：无 rawPhoto 字段，只返回比对结果与相似度分值。
 */
@Data
public class FaceVerifyResultVO {

    private Boolean passed;
    private BigDecimal score;
    private String sessionStatus;
    private String message;
}
