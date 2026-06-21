package cn.smu.edu.exam.service;

import cn.smu.edu.exam.domain.dto.ExamMonitorEventDTO;
import cn.smu.edu.exam.domain.vo.HeartbeatVO;

public interface MonitorService {

    /**
     * 学生端心跳（建议30秒调用一次）。
     * 更新 last_heartbeat_at；若之前 OFFLINE 则恢复为 ANSWERING。
     */
    HeartbeatVO heartbeat(Long publishId, Long studentId);

    /**
     * 上报监考异常事件（切屏/截图/复制）。
     * 超过阈值后 session_status=ABNORMAL，并向教师推送 Kafka 告警。
     */
    void reportEvent(Long publishId, Long studentId, ExamMonitorEventDTO dto);

    /**
     * 检测心跳超时学生，标记为 OFFLINE（XXL-Job 调用）。
     */
    void detectOfflineStudents();
}
