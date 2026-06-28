package cn.smu.edu.live.service;

import cn.smu.edu.live.domain.vo.ReplayVO;

/**
 * 课堂直播回放服务（S8-03）——按 {@code replay_visible} 控制可见性，生成回放播放地址。
 */
public interface ReplayService {

    /**
     * 获取课堂回放。
     *
     * @param lessonId 课堂 ID
     * @param roles    当前用户角色串（来自网关注入；教师/管理员不受 replay_visible 限制）
     */
    ReplayVO getReplay(Long lessonId, String roles);
}
