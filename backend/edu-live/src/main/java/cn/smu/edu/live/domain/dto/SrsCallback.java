package cn.smu.edu.live.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * SRS HTTP 回调载荷（on_publish / on_unpublish / on_dvr）。
 *
 * <p>SRS 以 server-to-server 方式 POST 此 JSON；处理成功须返回整型 {@code 0} 放行。
 * 字段参见 SRS http_hooks 文档；只取本服务关心的几项。
 */
@Data
public class SrsCallback {

    /** 事件动作：on_publish / on_unpublish / on_dvr。 */
    private String action;

    /** 应用名（SRS app），如 live。 */
    private String app;

    /** 流名 = 本系统 streamKey。 */
    private String stream;

    /** DVR 录制文件绝对路径（仅 on_dvr）。 */
    private String file;

    /** SRS 工作目录（仅 on_dvr）。 */
    private String cwd;

    @JsonProperty("client_id")
    private String clientId;

    private String ip;
}
