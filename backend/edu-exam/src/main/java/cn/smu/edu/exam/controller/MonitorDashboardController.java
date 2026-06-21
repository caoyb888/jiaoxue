package cn.smu.edu.exam.controller;

import cn.smu.edu.common.result.Result;
import cn.smu.edu.exam.domain.vo.MonitorDashboardVO;
import cn.smu.edu.exam.service.MonitorDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exam")
@RequiredArgsConstructor
public class MonitorDashboardController {

    private final MonitorDashboardService monitorDashboardService;

    /**
     * 监考状态大屏（教师端）。
     * 返回当前考试各学生实时状态分布 + 明细列表，教师端建议10s轮询。
     */
    @GetMapping("/monitor/list")
    public Result<MonitorDashboardVO> getDashboard(@RequestParam Long publishId) {
        return Result.ok(monitorDashboardService.getDashboard(publishId));
    }
}
