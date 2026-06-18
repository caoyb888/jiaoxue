package cn.smu.edu.jwxt.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 教务系统ID对照映射表
 * 双向唯一索引保证 O(1) 双向查找：jwxtId→localId 和 localId→jwxtId
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("jwxt_id_mapping")
public class JwxtIdMapping {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 数据类型：USER / DEPT / COURSE / CLASS */
    private String dataType;

    /** 教务系统原始ID */
    private String jwxtId;

    /** 本系统对应主键 */
    private Long localId;

    /** 最后同步的日志ID */
    private Long syncLogId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
