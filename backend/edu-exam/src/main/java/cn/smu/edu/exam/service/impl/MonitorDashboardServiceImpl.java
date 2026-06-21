package cn.smu.edu.exam.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.exam.domain.entity.ExamMonitor;
import cn.smu.edu.exam.domain.entity.ExamPublish;
import cn.smu.edu.exam.domain.vo.MonitorDashboardVO;
import cn.smu.edu.exam.domain.vo.MonitorItemVO;
import cn.smu.edu.exam.repository.ExamMonitorMapper;
import cn.smu.edu.exam.repository.ExamPublishMapper;
import cn.smu.edu.exam.service.MonitorDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitorDashboardServiceImpl implements MonitorDashboardService {

    private final ExamPublishMapper publishMapper;
    private final ExamMonitorMapper monitorMapper;

    @Override
    public MonitorDashboardVO getDashboard(Long publishId) {
        ExamPublish publish = publishMapper.selectById(publishId);
        if (publish == null) throw new BizException(ErrorCode.EXAM_NOT_FOUND);

        List<ExamMonitor> monitors = monitorMapper.selectByPublishId(publishId);

        // 状态分布统计
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("ANSWERING",  0L);
        distribution.put("VERIFYING",  0L);
        distribution.put("SUBMITTED",  0L);
        distribution.put("OFFLINE",    0L);
        distribution.put("ABNORMAL",   0L);
        monitors.stream()
                .collect(Collectors.groupingBy(ExamMonitor::getSessionStatus, Collectors.counting()))
                .forEach((k, v) -> distribution.merge(k, v, Long::sum));

        List<MonitorItemVO> items = monitors.stream().map(m -> {
            MonitorItemVO item = new MonitorItemVO();
            item.setStudentId(m.getStudentId());
            item.setSessionStatus(m.getSessionStatus());
            item.setLastHeartbeatAt(m.getLastHeartbeatAt());
            item.setTabSwitchCount(m.getTabSwitchCount());
            item.setScreenshotCount(m.getScreenshotCount());
            item.setCopyCount(m.getCopyCount());
            item.setAbnormalFlag(m.getAbnormalFlag());
            item.setFaceVerifyPassed(m.getFaceVerifyPassed());
            item.setFaceVerifyScore(m.getFaceVerifyScore());
            item.setSubmitTime(m.getSubmitTime());
            return item;
        }).collect(Collectors.toList());

        MonitorDashboardVO vo = new MonitorDashboardVO();
        vo.setPublishId(publishId);
        vo.setTotalStudents(monitors.size());
        vo.setStatusDistribution(distribution);
        vo.setStudents(items);
        return vo;
    }
}
