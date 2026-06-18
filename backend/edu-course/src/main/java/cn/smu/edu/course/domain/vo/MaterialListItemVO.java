package cn.smu.edu.course.domain.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialListItemVO {

    private Long id;
    private String title;
    private String fileType;
    private Integer pageCount;
    /** 0-转换中/待审核 1-可用 2-审核拒绝 */
    private Integer status;
    private Integer fileSizeKb;
    private LocalDateTime createdAt;
}
