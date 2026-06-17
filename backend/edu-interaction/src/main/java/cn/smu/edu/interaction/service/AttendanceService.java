package cn.smu.edu.interaction.service;

import cn.smu.edu.interaction.domain.dto.AttendDTO;
import cn.smu.edu.interaction.domain.vo.AttendResultVO;

public interface AttendanceService {

    /**
     * 【C1】学生签到：BloomFilter 去重 → Redis 队列入队 → 计数 → 立即返回
     */
    AttendResultVO attend(Long lessonId, Long studentId, AttendDTO dto);

    /**
     * 批量落库（由 @Scheduled 每 500ms 调用）
     */
    void flushQueueToDb(Long lessonId);
}
