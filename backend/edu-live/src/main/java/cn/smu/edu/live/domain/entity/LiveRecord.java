package cn.smu.edu.live.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 直播推流记录（{@code live_record}）。
 *
 * <p><b>C5 约束</b>：仅 {@code ONLINE_CLASS} 模式课堂创建此记录；{@code SLIDE_ONLY}
 * 线下课堂不创建（无 WebRTC / RTMP 推流）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("live_record")
public class LiveRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lessonId;

    /** RTMP 推流密钥。 */
    private String streamKey;

    /** RTMP 推流地址。 */
    private String pushUrl;

    /** HLS 拉流播放地址。 */
    private String playUrl;

    /** 录播文件 MinIO 路径。 */
    private String replayPath;

    private Integer durationSec;

    /** 状态：0-待推流 1-推流中 2-已结束 3-已生成回放。 */
    private Integer status;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
}
