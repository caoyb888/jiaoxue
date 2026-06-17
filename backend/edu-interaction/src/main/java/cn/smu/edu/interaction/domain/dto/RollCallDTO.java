package cn.smu.edu.interaction.domain.dto;

import lombok.Data;

@Data
public class RollCallDTO {

    /** 点名人数（默认 1） */
    private int count = 1;

    /** 是否排除缺勤学生（true = 仅从已签到学生中抽） */
    private boolean excludeAbsent = true;

    /** 点名样式：random/spotlight/racing */
    private String style = "random";
}
