package cn.smu.edu.exam.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 监考状态大屏 VO。
 * 含各 session_status 实时分布统计 + 全部学生监考明细列表。
 */
@Data
public class MonitorDashboardVO {

    private Long publishId;
    private int totalStudents;

    /** session_status → count（ANSWERING/VERIFYING/SUBMITTED/OFFLINE/ABNORMAL） */
    private Map<String, Long> statusDistribution;

    /** 全部学生监考明细 */
    private List<MonitorItemVO> students;
}
