package cn.smu.edu.stat.domain.vo;

import java.util.List;

/**
 * 院系历史教学统计（S7-05），按 {@code period} 粒度（day/week/month）在
 * {@code [fromDate, toDate]} 时间窗内分桶聚合。
 *
 * <p>数据源为 ClickHouse {@code lesson_event_log} 明细层，查询 WHERE 子句强制带
 * {@code stat_date} 分区键（CLAUDE.md §7.4）。
 *
 * @param deptId   院系 ID
 * @param period   聚合粒度：day / week / month
 * @param fromDate 起始日期（含，yyyy-MM-dd）
 * @param toDate   截止日期（含，yyyy-MM-dd）
 * @param buckets  各时间桶统计，按 periodStart 升序；无数据的桶不补零
 */
public record DeptHistoryVO(
        Long deptId,
        String period,
        String fromDate,
        String toDate,
        List<DeptPeriodStatVO> buckets) {
}
