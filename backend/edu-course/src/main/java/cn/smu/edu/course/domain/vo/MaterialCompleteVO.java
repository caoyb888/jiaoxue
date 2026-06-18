package cn.smu.edu.course.domain.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialCompleteVO {

    private Long materialId;
    /** 0-转换中 1-可用（mp4待审核时也为0） */
    private Integer status;
    private String message;
}
