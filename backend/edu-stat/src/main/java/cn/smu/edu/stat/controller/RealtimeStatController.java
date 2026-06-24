package cn.smu.edu.stat.controller;

import cn.smu.edu.common.result.Result;
import cn.smu.edu.stat.domain.vo.LessonRealtimeVO;
import cn.smu.edu.stat.domain.vo.RealtimeOverviewVO;
import cn.smu.edu.stat.service.RealtimeStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 实时统计 API（S7-03）—— 大屏与课堂实时数据，读 Redis 5 分钟滑动窗口聚合（S7-02）。
 *
 * <p>均为只读接口，无写操作；数据来自 Redis，不查库，响应毫秒级，适合大屏 10s 轮询。
 */
@RestController
@RequestMapping("/api/v1/stat/realtime")
@RequiredArgsConstructor
public class RealtimeStatController {

    private final RealtimeStatService realtimeStatService;

    /** 大屏实时概览：活跃课堂数、在线人数、各事件桶发生量（最近 5 分钟）。 */
    @GetMapping("/overview")
    public Result<RealtimeOverviewVO> overview() {
        return Result.ok(realtimeStatService.overview());
    }

    /** 指定课堂的实时统计：在线人数、各事件桶发生量（最近 5 分钟）。 */
    @GetMapping("/lesson/{lessonId}")
    public Result<LessonRealtimeVO> lessonRealtime(@PathVariable Long lessonId) {
        return Result.ok(realtimeStatService.lessonRealtime(lessonId));
    }
}
