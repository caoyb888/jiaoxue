package cn.smu.edu.jwxt.service;

import cn.smu.edu.jwxt.domain.entity.JwxtIdMapping;

import java.util.List;

/**
 * 教务 ID 双向对照映射服务（S7-08）。
 *
 * <p>基于 {@code jwxt_id_mapping} 双向唯一索引（{@code uk_type_jwxt_id} /
 * {@code uk_type_local_id}）实现 O(1) 双向查找：增量同步靠它判断教务记录是
 * 新增还是更新，并维护教务ID ↔ 本系统主键的对应关系。
 */
public interface JwxtMappingService {

    /** 教务ID → 本系统主键；无映射返回 {@code null}。 */
    Long resolveLocalId(String dataType, String jwxtId);

    /** 本系统主键 → 教务ID；无映射返回 {@code null}。 */
    String resolveJwxtId(String dataType, Long localId);

    /**
     * 批量去重 upsert 映射（命中任一唯一键则更新 local_id/sync_log_id）。
     *
     * @param mappings 待写入映射，空列表直接返回 0
     * @return 受影响行数
     */
    int saveMappings(List<JwxtIdMapping> mappings);

    /** 单条 upsert 便捷方法（增量同步逐条登记映射时使用）。 */
    int upsert(String dataType, String jwxtId, Long localId, Long syncLogId);
}
