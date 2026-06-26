package cn.smu.edu.stat.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.stat.domain.dto.WarnQueryDTO;
import cn.smu.edu.stat.domain.vo.WarnVO;
import cn.smu.edu.stat.service.WarnQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教学预警列表 API（S7-07），供管理员预警列表页（S7-14）。
 *
 * <p>列表为只读，查 MySQL {@code warn_event}；标记处理为写操作，加 {@link OperationLog} 留痕。
 * 角色鉴权由网关统一处理，本服务信任网关已通过 JWT 校验。
 */
@RestController
@RequestMapping("/api/v1/stat/warn")
@RequiredArgsConstructor
public class WarnController {

    private final WarnQueryService warnQueryService;

    /** 分页查询预警列表，支持 warnType / status / deptId 过滤。 */
    @GetMapping("/list")
    public Result<PageResult<WarnVO>> list(WarnQueryDTO query) {
        return Result.ok(warnQueryService.pageWarns(query));
    }

    /**
     * 标记预警处理状态（一键处理/忽略）。
     *
     * @param id     预警 ID
     * @param status 目标状态：1已处理 / 2忽略
     */
    @PutMapping("/{id}/handle")
    @OperationLog(module = "stat", operation = "处理教学预警")
    public Result<Void> handle(@PathVariable Long id, @RequestParam Integer status) {
        warnQueryService.handleWarn(id, status, UserContext.getUserId());
        return Result.ok();
    }
}
