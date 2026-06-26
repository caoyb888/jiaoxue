package cn.smu.edu.stat.domain.dto;

import lombok.Data;

/**
 * 预警列表查询条件（S7-07）。全部可选；分页缺省第 1 页、每页 20 条。
 */
@Data
public class WarnQueryDTO {

    /** 预警类型过滤：LOW_ATTEND/ZERO_ACTIVE/FREQUENT_ABSENCE。 */
    private String warnType;

    /** 处理状态过滤：0未处理/1已处理/2忽略。 */
    private Integer status;

    /** 院系过滤。 */
    private Long deptId;

    private Integer page = 1;
    private Integer size = 20;
}
