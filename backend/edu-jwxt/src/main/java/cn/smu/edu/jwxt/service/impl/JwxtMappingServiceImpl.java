package cn.smu.edu.jwxt.service.impl;

import cn.smu.edu.jwxt.domain.entity.JwxtIdMapping;
import cn.smu.edu.jwxt.repository.JwxtIdMappingMapper;
import cn.smu.edu.jwxt.service.JwxtMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link JwxtMappingService} 实现，委托 {@link JwxtIdMappingMapper} 的双向唯一索引查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwxtMappingServiceImpl implements JwxtMappingService {

    private final JwxtIdMappingMapper idMappingMapper;

    @Override
    public Long resolveLocalId(String dataType, String jwxtId) {
        return idMappingMapper.selectLocalIdByJwxtId(dataType, jwxtId);
    }

    @Override
    public String resolveJwxtId(String dataType, Long localId) {
        return idMappingMapper.selectJwxtIdByLocalId(dataType, localId);
    }

    @Override
    public int saveMappings(List<JwxtIdMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return 0;
        }
        int affected = idMappingMapper.batchUpsert(mappings);
        log.info("jwxt_id_mapping 批量 upsert: 提交={}, 受影响行={}", mappings.size(), affected);
        return affected;
    }

    @Override
    public int upsert(String dataType, String jwxtId, Long localId, Long syncLogId) {
        JwxtIdMapping mapping = JwxtIdMapping.builder()
                .dataType(dataType)
                .jwxtId(jwxtId)
                .localId(localId)
                .syncLogId(syncLogId)
                .build();
        return idMappingMapper.batchUpsert(List.of(mapping));
    }
}
