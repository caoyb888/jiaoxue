package cn.smu.edu.course.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("course_material")
public class CourseMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teacherId;
    private String title;

    /** 原始文件类型：pptx/pdf/docx/mp4 */
    private String fileType;

    private String originalPath;

    /** 转换后图片序列目录路径（MinIO），转换完成后填入 */
    private String slideDir;

    private Integer pageCount;
    private Integer fileSizeKb;

    /** 0-待转换/待审核 1-可用 2-审核拒绝 */
    private Integer status;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
