package cn.smu.edu.live.domain.vo;

/**
 * 课堂直播配置（S8-01）。
 *
 * <p><b>C5 红线</b>：{@code SLIDE_ONLY} 线下课堂 {@code webrtcEnabled=false}、
 * {@code rtmpEnabled=false}、推/拉流地址均为 {@code null}，前端据此不发起任何 WebRTC 连接；
 * 仅 {@code ONLINE_CLASS} 线上课堂开启推流并下发地址。
 *
 * @param lessonId       课堂 ID
 * @param liveMode       直播模式 SLIDE_ONLY / ONLINE_CLASS
 * @param webrtcEnabled  是否启用 WebRTC（SLIDE_ONLY 恒 false）
 * @param rtmpEnabled    是否启用 RTMP 推流（SLIDE_ONLY 恒 false）
 * @param streamKey      推流密钥（SLIDE_ONLY 为 null）
 * @param pushUrl        RTMP 推流地址（SLIDE_ONLY 为 null）
 * @param playUrl        HLS 拉流地址（SLIDE_ONLY 为 null）
 * @param status         直播记录状态 0待推流/1推流中/2已结束/3已生成回放（SLIDE_ONLY 为 null）
 */
public record LiveConfigVO(
        Long lessonId,
        String liveMode,
        boolean webrtcEnabled,
        boolean rtmpEnabled,
        String streamKey,
        String pushUrl,
        String playUrl,
        Integer status) {

    /** SLIDE_ONLY 线下课堂：关闭一切流媒体（C5）。 */
    public static LiveConfigVO slideOnly(Long lessonId) {
        return new LiveConfigVO(lessonId, "SLIDE_ONLY", false, false, null, null, null, null);
    }
}
