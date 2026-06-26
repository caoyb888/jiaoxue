package cn.smu.edu.jwxt.service;

/**
 * 教务增量同步编排（S7-09）。
 *
 * <p>从当前生效的厂商适配器分批增量拉取 → 暂存 jwxt_raw_data → 经双向映射对照判断
 * 新增/更新 → 全程记入 jwxt_sync_log。分批防超时。
 */
public interface JwxtIncrementalSyncService {

    /**
     * 执行一次增量同步。
     *
     * @param triggeredBy 触发方式 SCHEDULE / MANUAL
     * @return 本次 jwxt_sync_log 记录 ID
     */
    long incrementalSync(String triggeredBy);
}
