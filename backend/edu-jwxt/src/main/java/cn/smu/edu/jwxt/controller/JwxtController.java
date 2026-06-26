package cn.smu.edu.jwxt.controller;

import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.jwxt.service.JwxtFullSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教务同步管理 API（S7-10）。角色鉴权由网关统一处理（管理员）。
 */
@RestController
@RequestMapping("/api/v1/jwxt")
@RequiredArgsConstructor
public class JwxtController {

    private final JwxtFullSyncService fullSyncService;

    /**
     * 触发教务全量同步（分批 500 条），完成后通知触发管理员。
     *
     * @return 本次同步日志 ID（可据此查 jwxt_sync_log 进度/结果）
     */
    @PostMapping("/sync/full")
    @OperationLog(module = "jwxt", operation = "触发教务全量同步")
    public Result<Long> fullSync() {
        long syncLogId = fullSyncService.fullSync(UserContext.getUserId());
        return Result.ok(syncLogId);
    }
}
