package cn.smu.edu.jwxt.adapter;

import java.time.LocalDate;
import java.util.List;

/**
 * 教务系统数据源适配层（S7-09）。
 *
 * <p>屏蔽正方（zhengfang）/ 强智（qiangzhi）等不同厂商的接口差异，ETL 编排只依赖本接口。
 * 增量拉取按 {@code (dataType, since)} 过滤，并以 {@code (offset, limit)} 分页，
 * <b>分批防超时</b>（单批不宜过大，老校园网/教务接口易超时）。
 */
public interface JwxtDataSource {

    /** 厂商标识：zhengfang / qiangzhi。 */
    String vendor();

    /**
     * 增量分页拉取某类数据。
     *
     * @param dataType 数据类型 STUDENT/DEPT/COURSE/CLASS
     * @param since    增量基准日期（拉取此日期之后变更的数据）
     * @param offset   分页偏移
     * @param limit    单批条数
     * @return 该批归一化记录；返回空或不足 {@code limit} 视为已到末页
     */
    List<JwxtRecord> fetchIncremental(String dataType, LocalDate since, int offset, int limit);
}
