package cn.smu.edu.jwxt.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 教务原始数据暂存表（ETL 过渡层）。
 *
 * <p>增量/全量同步先把教务系统拉来的原始 JSON 落此表（append-only），
 * 再异步解析、对照映射、写入业务库；成功处理 90 天后清理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("jwxt_raw_data")
public class JwxtRawData {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 同步日志ID → jwxt_sync_log.id。 */
    private Long syncLogId;

    /** 数据类型：STUDENT / DEPT / COURSE / CLASS。 */
    private String dataType;

    /** 教务系统原始ID（便于快速对照）。 */
    private String jwxtId;

    /** 教务系统原始数据（JSON 字符串）。 */
    private String rawJson;

    /** 处理状态：0-待处理 1-成功 2-失败。 */
    private Integer status;

    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
