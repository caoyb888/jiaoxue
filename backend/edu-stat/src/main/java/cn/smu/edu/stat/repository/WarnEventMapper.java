package cn.smu.edu.stat.repository;

import cn.smu.edu.stat.domain.entity.WarnEvent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * {@code warn_event} 数据访问层。
 */
@Mapper
public interface WarnEventMapper extends BaseMapper<WarnEvent> {

    /**
     * 批量去重 upsert（架构约束：禁止循环单条 INSERT）。
     *
     * <p>命中去重键 {@code uk_warn_dedupe} 时只刷新指标/阈值/详情，
     * <b>不重置 {@code status}</b>——已处理/已忽略的预警当天重复触发仍保持处理状态。
     *
     * @param list 待写入预警（应非空）
     * @return 受影响行数
     */
    @Insert("""
            <script>
            INSERT INTO warn_event
                (warn_type, target_type, target_id, lesson_id, class_id, dept_id, teacher_id,
                 stat_date, metric_value, threshold_value, detail, status)
            VALUES
            <foreach collection="list" item="w" separator=",">
                (#{w.warnType}, #{w.targetType}, #{w.targetId}, #{w.lessonId}, #{w.classId},
                 #{w.deptId}, #{w.teacherId}, #{w.statDate}, #{w.metricValue}, #{w.thresholdValue},
                 #{w.detail}, 0)
            </foreach>
            ON DUPLICATE KEY UPDATE
                metric_value    = VALUES(metric_value),
                threshold_value = VALUES(threshold_value),
                detail          = VALUES(detail),
                updated_at      = NOW()
            </script>
            """)
    int upsertBatch(@Param("list") List<WarnEvent> list);
}
