package cn.smu.edu.exam.repository;

import cn.smu.edu.exam.domain.entity.ExamMonitor;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ExamMonitorMapper extends BaseMapper<ExamMonitor> {

    @Select("SELECT * FROM exam_monitor WHERE publish_id = #{publishId} AND student_id = #{studentId} LIMIT 1")
    ExamMonitor selectByPublishAndStudent(@Param("publishId") Long publishId,
                                          @Param("studentId") Long studentId);

    @Select("SELECT * FROM exam_monitor WHERE publish_id = #{publishId} ORDER BY created_at ASC")
    List<ExamMonitor> selectByPublishId(@Param("publishId") Long publishId);

    @Update("UPDATE exam_monitor SET last_heartbeat_at = NOW(), session_status = #{status} " +
            "WHERE publish_id = #{publishId} AND student_id = #{studentId}")
    int updateHeartbeat(@Param("publishId") Long publishId,
                        @Param("studentId") Long studentId,
                        @Param("status") String status);

    /** 统计各 session_status 数量，用于大屏 */
    @Select("SELECT session_status, COUNT(*) AS cnt FROM exam_monitor " +
            "WHERE publish_id = #{publishId} GROUP BY session_status")
    List<Map<String, Object>> countByStatus(@Param("publishId") Long publishId);

    /** 心跳超时检测：last_heartbeat_at 超过 seconds 秒未更新的 ANSWERING/VERIFYING 记录 */
    @Select("SELECT * FROM exam_monitor WHERE session_status IN ('ANSWERING', 'VERIFYING') " +
            "AND last_heartbeat_at < DATE_SUB(NOW(), INTERVAL #{seconds} SECOND)")
    List<ExamMonitor> selectStaleHeartbeats(@Param("seconds") int seconds);
}
