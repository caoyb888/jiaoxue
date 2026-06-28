package cn.smu.edu.live.service;

import cn.smu.edu.live.domain.vo.LiveConfigVO;

/**
 * 课堂直播服务（S8-01）——按课堂 {@code live_mode} 分级下发直播配置，落实 C5 红线。
 */
public interface LiveService {

    /**
     * 开启/获取课堂直播配置。
     *
     * <ul>
     *   <li>SLIDE_ONLY：不创建 live_record，返回全关闭配置（C5）；</li>
     *   <li>ONLINE_CLASS：生成 streamKey + 推/拉流地址，upsert live_record（待推流）。</li>
     * </ul>
     *
     * @param lessonId   课堂 ID
     * @param operatorId 操作人（教师）ID
     */
    LiveConfigVO startLive(Long lessonId, Long operatorId);

    /** 查询课堂当前直播配置（不产生副作用）。 */
    LiveConfigVO getLiveConfig(Long lessonId);

    /** 结束直播：标记 live_record 结束并记录时长（仅 ONLINE_CLASS 有意义）。 */
    LiveConfigVO stopLive(Long lessonId, Long operatorId);
}
