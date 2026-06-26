package cn.smu.edu.stat.controller;

import cn.smu.edu.common.result.Result;
import cn.smu.edu.stat.domain.vo.ClassHistoryVO;
import cn.smu.edu.stat.domain.vo.DeptHistoryVO;
import cn.smu.edu.stat.service.HistoryStatService;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 历史统计 API（S7-04）—— 班级历史教学趋势，读 ClickHouse 逐日聚合。
 *
 * <p>只读接口；与实时统计 {@link RealtimeStatController}（读 Redis）分工：
 * 本接口面向班级历史图表页（S7-13），数据来自 ClickHouse OLAP 层。
 */
@RestController
@RequestMapping("/api/v1/stat/history")
@RequiredArgsConstructor
public class HistoryStatController {

    /** 回溯天数上限（与 ClickHouse 分区裁剪、前端图表可读性折中）。 */
    private static final int MAX_DAYS = 180;
    private static final int DEFAULT_DAYS = 30;

    /** 合法聚合粒度白名单；非法值兜底为 day。 */
    private static final Set<String> VALID_PERIODS = Set.of("day", "week", "month");
    private static final String DEFAULT_PERIOD = "day";

    private final HistoryStatService historyStatService;

    /**
     * 班级近 N 天逐日教学统计。
     *
     * @param classId 班级 ID
     * @param days    回溯天数，缺省 30，规整到 [1, 180]
     */
    @GetMapping("/class/{classId}")
    public Result<ClassHistoryVO> classHistory(
            @PathVariable Long classId,
            @RequestParam(defaultValue = "" + DEFAULT_DAYS) int days) {
        int normalized = Math.min(Math.max(days, 1), MAX_DAYS);
        return Result.ok(historyStatService.classHistory(classId, normalized));
    }

    /**
     * 院系近 N 天教学统计，按 period 粒度分桶。
     *
     * @param deptId 院系 ID
     * @param period 聚合粒度 day/week/month，缺省 day，非法值兜底 day
     * @param days   回溯天数，缺省 30，规整到 [1, 180]
     */
    @GetMapping("/dept/{deptId}")
    public Result<DeptHistoryVO> deptHistory(
            @PathVariable Long deptId,
            @RequestParam(defaultValue = DEFAULT_PERIOD) String period,
            @RequestParam(defaultValue = "" + DEFAULT_DAYS) int days) {
        String normalizedPeriod = VALID_PERIODS.contains(period) ? period : DEFAULT_PERIOD;
        int normalizedDays = Math.min(Math.max(days, 1), MAX_DAYS);
        return Result.ok(historyStatService.deptHistory(deptId, normalizedPeriod, normalizedDays));
    }
}
