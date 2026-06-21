package cn.smu.edu.interaction.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 学生简要信息（用于签到/点名结果展示姓名，读自共享库 sys_user） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentBriefVO {
    private Long id;
    private String realName;
    private String studentNo;
}
