package cn.smu.edu.jwxt.service;

/**
 * 教务全量同步（S7-10）——分批拉取全部教务数据，完成后通知触发管理员。
 */
public interface JwxtFullSyncService {

    /**
     * 执行一次全量同步。
     *
     * @param operatorId 触发的管理员用户ID（完成后微信/站内通知该用户）；可空（如系统触发）
     * @return 本次 jwxt_sync_log 记录 ID
     */
    long fullSync(Long operatorId);
}
