package cn.smu.edu.jwxt.service;

import cn.smu.edu.jwxt.domain.entity.JwxtRawData;

import java.util.List;

/**
 * 教务原始数据暂存服务（S7-08）——{@code jwxt_raw_data} ETL 过渡层的读写逻辑。
 */
public interface JwxtRawDataService {

    /**
     * 批量落库原始数据（append-only）。
     *
     * @param rows 原始记录，空列表直接返回 0
     * @return 入库条数
     */
    int stage(List<JwxtRawData> rows);

    /** 按数据类型拉取一批待处理（status=0）原始数据。 */
    List<JwxtRawData> fetchPending(String dataType, int limit);

    /** 批量标记处理成功（status=1，清空 error_msg）。 */
    int markSuccess(List<Long> ids);

    /** 批量标记处理失败（status=2，记录原因）。 */
    int markFailed(List<Long> ids, String errorMsg);
}
