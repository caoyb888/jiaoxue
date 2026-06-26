package cn.smu.edu.jwxt.service.impl;

import cn.smu.edu.jwxt.service.JwxtIncrementalSyncService;
import cn.smu.edu.jwxt.service.JwxtSyncExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * {@link JwxtIncrementalSyncService} 实现，委托 {@link JwxtSyncExecutor}：
 * 类型 INCREMENTAL、增量基准取上次成功同步日期、无成功后回调。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwxtIncrementalSyncServiceImpl implements JwxtIncrementalSyncService {

    private final JwxtSyncExecutor syncExecutor;

    @Override
    public long incrementalSync(String triggeredBy) {
        return syncExecutor.runSync("INCREMENTAL", syncExecutor.lastSuccessfulSyncDate(), triggeredBy, null);
    }
}
