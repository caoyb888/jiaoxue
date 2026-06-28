package cn.smu.edu.live.domain.vo;

/**
 * 课堂直播回放信息（S8-03）。
 *
 * @param lessonId    课堂 ID
 * @param available   回放是否可播放（已生成且当前用户可见）
 * @param visible     回放对当前用户是否可见（replay_visible=0 时学生不可见）
 * @param replayUrl   回放播放地址（CDN 或 MinIO 预签名；不可见/未就绪为 null）
 * @param durationSec 回放时长（秒）
 */
public record ReplayVO(
        Long lessonId,
        boolean available,
        boolean visible,
        String replayUrl,
        Integer durationSec) {

    /** 回放对学生隐藏（replay_visible=0）。 */
    public static ReplayVO hidden(Long lessonId) {
        return new ReplayVO(lessonId, false, false, null, null);
    }

    /** 回放尚未生成（可见但无文件）。 */
    public static ReplayVO notReady(Long lessonId) {
        return new ReplayVO(lessonId, false, true, null, null);
    }
}
