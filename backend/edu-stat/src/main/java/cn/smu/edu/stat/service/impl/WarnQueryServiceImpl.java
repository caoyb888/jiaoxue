package cn.smu.edu.stat.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.stat.domain.dto.WarnQueryDTO;
import cn.smu.edu.stat.domain.entity.WarnEvent;
import cn.smu.edu.stat.domain.vo.WarnVO;
import cn.smu.edu.stat.repository.WarnEventMapper;
import cn.smu.edu.stat.service.WarnQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * {@link WarnQueryService} 实现，基于 MyBatis-Plus 查询 MySQL {@code warn_event}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarnQueryServiceImpl implements WarnQueryService {

    /** 合法处理状态：1已处理 / 2忽略（0 未处理不作为处理目标）。 */
    private static final Set<Integer> HANDLE_STATUS = Set.of(1, 2);
    private static final int MAX_PAGE_SIZE = 100;

    private final WarnEventMapper warnEventMapper;

    @Override
    public PageResult<WarnVO> pageWarns(WarnQueryDTO query) {
        long page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        long size = query.getSize() == null || query.getSize() < 1 ? 20
                : Math.min(query.getSize(), MAX_PAGE_SIZE);

        LambdaQueryWrapper<WarnEvent> wrapper = new LambdaQueryWrapper<WarnEvent>()
                .eq(StringUtils.hasText(query.getWarnType()), WarnEvent::getWarnType, query.getWarnType())
                .eq(query.getStatus() != null, WarnEvent::getStatus, query.getStatus())
                .eq(query.getDeptId() != null, WarnEvent::getDeptId, query.getDeptId())
                .orderByDesc(WarnEvent::getStatDate)
                .orderByDesc(WarnEvent::getId);

        Page<WarnEvent> result = warnEventMapper.selectPage(new Page<>(page, size), wrapper);
        List<WarnVO> vos = result.getRecords().stream().map(WarnQueryServiceImpl::toVO).toList();
        return PageResult.of(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public void handleWarn(long id, int status, Long handledBy) {
        if (!HANDLE_STATUS.contains(status)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "处理状态非法，仅支持 1(已处理)/2(忽略)");
        }
        WarnEvent existing = warnEventMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "预警事件不存在");
        }
        WarnEvent update = new WarnEvent();
        update.setId(id);
        update.setStatus(status);
        update.setHandledBy(handledBy);
        update.setHandledAt(LocalDateTime.now());
        warnEventMapper.updateById(update);
        log.info("预警处理: id={}, status={}, handledBy={}", id, status, handledBy);
    }

    private static WarnVO toVO(WarnEvent e) {
        return new WarnVO(
                e.getId(), e.getWarnType(), e.getTargetType(), e.getTargetId(),
                e.getLessonId(), e.getClassId(), e.getDeptId(), e.getTeacherId(),
                e.getStatDate() == null ? null : e.getStatDate().toString(),
                e.getMetricValue(), e.getThresholdValue(), e.getDetail(), e.getStatus(),
                e.getCreatedAt() == null ? null : e.getCreatedAt().toString());
    }
}
