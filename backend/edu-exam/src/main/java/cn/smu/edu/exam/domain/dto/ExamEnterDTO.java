package cn.smu.edu.exam.domain.dto;

import lombok.Data;

/** 进入考试请求（密码可选）。人脸核验通过 S5-04 单独接口完成。 */
@Data
public class ExamEnterDTO {
    /** 明文密码（无密码时传 null） */
    private String password;
}
