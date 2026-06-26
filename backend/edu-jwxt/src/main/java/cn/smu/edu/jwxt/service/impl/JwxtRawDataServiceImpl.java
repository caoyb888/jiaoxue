package cn.smu.edu.jwxt.service.impl;

import cn.smu.edu.jwxt.domain.entity.JwxtRawData;
import cn.smu.edu.jwxt.repository.JwxtRawDataMapper;
import cn.smu.edu.jwxt.service.JwxtRawDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@link JwxtRawDataService} 实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwxtRawDataServiceImpl implements JwxtRawDataService {

    /** 状态：0-待处理 1-成功 2-失败。 */
    private static final int STATUS_SUCCESS = 1;
    private static final int STATUS_FAILED = 2;

    private final JwxtRawDataMapper rawDataMapper;

    @Override
    public int stage(List<JwxtRawData> rows) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        int inserted = rawDataMapper.batchInsert(rows);
        log.info("jwxt_raw_data 暂存: 入库={}", inserted);
        return inserted;
    }

    @Override
    public List<JwxtRawData> fetchPending(String dataType, int limit) {
        return rawDataMapper.selectPendingByType(dataType, limit);
    }

    @Override
    public int markSuccess(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return rawDataMapper.updateStatusByIds(ids, STATUS_SUCCESS, null);
    }

    @Override
    public int markFailed(List<Long> ids, String errorMsg) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return rawDataMapper.updateStatusByIds(ids, STATUS_FAILED, errorMsg);
    }
}
