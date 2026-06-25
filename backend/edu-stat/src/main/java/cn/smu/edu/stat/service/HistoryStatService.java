package cn.smu.edu.stat.service;

import cn.smu.edu.stat.domain.vo.ClassHistoryVO;

/**
 * 历史统计查询服务（S7-04）——查 ClickHouse 历史明细做逐日聚合。
 *
 * <p>与实时统计（{@link RealtimeStatService}，读 Redis 5 分钟窗口）互补：
 * 本服务面向班级历史趋势图表，数据来自 ClickHouse OLAP 层。
 */
public interface HistoryStatService {

    /**
     * 班级近 {@code days} 天的逐日教学统计。
     *
     * @param classId 班级 ID
     * @param days    回溯天数（含今天），调用方应保证已规整到合理区间
     * @return 逐日聚合结果（按日期升序）
     */
    ClassHistoryVO classHistory(long classId, int days);
}
