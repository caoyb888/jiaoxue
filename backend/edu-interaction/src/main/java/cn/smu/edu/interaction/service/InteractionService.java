package cn.smu.edu.interaction.service;

import cn.smu.edu.interaction.domain.dto.*;
import cn.smu.edu.interaction.domain.vo.RollCallVO;

import java.util.List;
import java.util.Map;

public interface InteractionService {

    /** S3-06 弹幕：入库 + WebSocket 广播 */
    void sendBarrage(Long lessonId, Long studentId, BarrageDTO dto);

    /** S3-06 教师屏蔽弹幕 */
    void blockBarrage(Long barrageId, Long teacherId);

    /** S3-07 随机点名 */
    RollCallVO rollCall(Long lessonId, Long teacherId, RollCallDTO dto);

    /** S3-08 课件反馈 */
    void slideFeedback(Long lessonId, Long studentId, SlideFeedbackDTO dto);

    /** S3-08 热点页面统计 */
    List<Map<String, Object>> slideFeedbackStats(Long lessonId);

    /** S3-09 课堂积分 */
    void addScore(Long lessonId, Long classId, Long operatorId, ClassScoreDTO dto);
}
