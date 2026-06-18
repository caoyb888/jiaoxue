package cn.smu.edu.jwxt.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 教务系统同步日志（全量/增量ETL执行记录）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("jwxt_sync_log")
public class JwxtSyncLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 同步类型：FULL / INCREMENTAL */
    private String syncType;

    private LocalDate syncDate;

    private Integer studentCnt;
    private Integer deptCnt;
    private Integer courseCnt;
    private Integer successCnt;
    private Integer failedCnt;

    /** 0-进行中 1-成功 2-部分失败 3-完全失败 */
    private Integer status;

    private String errorMsg;
    private Long costMs;

    /** 触发方式：SCHEDULE / MANUAL */
    private String triggeredBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime finishedAt;
}
