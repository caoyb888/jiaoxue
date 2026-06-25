package cn.smu.edu.stat.domain.vo;

import java.util.List;

/**
 * 班级历史教学统计（S7-04），覆盖 {@code [fromDate, toDate]} 时间窗内的逐日聚合。
 *
 * <p>数据源为 ClickHouse {@code lesson_event_log} 明细层，查询 WHERE 子句强制带
 * {@code stat_date} 分区键（CLAUDE.md §7.4），避免全表扫描。
 *
 * @param classId  班级 ID
 * @param fromDate 起始日期（含，yyyy-MM-dd）
 * @param toDate   截止日期（含，yyyy-MM-dd）
 * @param daily    逐日统计，按日期升序；无数据的日期不补零（前端按需补齐）
 */
public record ClassHistoryVO(
        Long classId,
        String fromDate,
        String toDate,
        List<ClassDailyStatVO> daily) {
}
