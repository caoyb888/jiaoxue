package cn.smu.edu.stat.controller;

import cn.smu.edu.common.result.Result;
import cn.smu.edu.stat.domain.vo.ClassHistoryVO;
import cn.smu.edu.stat.service.HistoryStatService;
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
}
