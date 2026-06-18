package cn.smu.edu.exam.service;

import cn.smu.edu.exam.domain.vo.MonitorDashboardVO;

public interface MonitorDashboardService {

    /**
     * 获取考试监考大屏数据：状态分布 + 全部学生监考明细。
     * 教师端实时轮询（建议10s一次）。
     */
    MonitorDashboardVO getDashboard(Long publishId);
}
