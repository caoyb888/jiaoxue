package cn.smu.edu.user.domain.vo;

import lombok.Data;
import java.util.List;

@Data
public class DeptVO {
    private Long id;
    private String deptCode;
    private String deptName;
    private Long parentId;
    private Integer deptType;
    private Integer level;
    private Integer sortOrder;
    private List<DeptVO> children;
}
