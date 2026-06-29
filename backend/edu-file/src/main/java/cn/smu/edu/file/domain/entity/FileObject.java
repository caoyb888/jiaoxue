package cn.smu.edu.file.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件对象（{@code file_object}）。
 */
@Data
@TableName("file_object")
public class FileObject {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long uploaderId;
    private String fileName;
    private String bucketName;
    private String objectPath;

    /** 业务类型：SLIDE/VIDEO/AUDIO/EXAM_ATTACH/ARCHIVE_PHOTO。 */
    private String bizType;

    /** 存储级别：HOT-热存储 COLD-冷存储。 */
    private String storageClass;

    /** 生命周期阶段：ACTIVE-活跃 ARCHIVED-归档 DELETED-已删除。 */
    private String lifecycleStage;

    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
