package cn.smu.edu.stat.service;

import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.stat.domain.dto.WarnQueryDTO;
import cn.smu.edu.stat.domain.vo.WarnVO;

/**
 * 预警查询/处理服务（S7-07）——读写 MySQL {@code warn_event}。
 */
public interface WarnQueryService {

    /** 分页查询预警列表，支持类型/状态/院系过滤，按统计日期与 ID 倒序。 */
    PageResult<WarnVO> pageWarns(WarnQueryDTO query);

    /**
     * 标记预警处理状态（管理员一键处理/忽略）。
     *
     * @param id        预警 ID
     * @param status    目标状态：1已处理/2忽略
     * @param handledBy 处理人 ID（来自登录态，可空）
     */
    void handleWarn(long id, int status, Long handledBy);
}
