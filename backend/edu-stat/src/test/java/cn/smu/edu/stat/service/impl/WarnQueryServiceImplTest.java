package cn.smu.edu.stat.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.stat.domain.dto.WarnQueryDTO;
import cn.smu.edu.stat.domain.entity.WarnEvent;
import cn.smu.edu.stat.domain.vo.WarnVO;
import cn.smu.edu.stat.repository.WarnEventMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarnQueryServiceImplTest {

    @Mock
    WarnEventMapper warnEventMapper;

    private WarnEvent sample() {
        return WarnEvent.builder()
                .id(1L).warnType("LOW_ATTEND").targetType("LESSON").targetId(1001L)
                .lessonId(1001L).classId(100L).deptId(10L).teacherId(50L)
                .statDate(LocalDate.of(2026, 6, 26)).metricValue(5).thresholdValue(15)
                .detail("低考勤").status(0).build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageWarns_shouldMapEntitiesToVOAndReturnPageResult() {
        Page<WarnEvent> page = new Page<>(1, 20);
        page.setRecords(List.of(sample()));
        page.setTotal(1);
        when(warnEventMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        WarnQueryDTO query = new WarnQueryDTO();
        query.setWarnType("LOW_ATTEND");
        query.setStatus(0);

        PageResult<WarnVO> result = new WarnQueryServiceImpl(warnEventMapper).pageWarns(query);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.list()).hasSize(1);
        WarnVO vo = result.list().get(0);
        assertThat(vo.warnType()).isEqualTo("LOW_ATTEND");
        assertThat(vo.statDate()).isEqualTo("2026-06-26");
        assertThat(vo.metricValue()).isEqualTo(5);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageWarns_shouldClampPageSizeTo100() {
        Page<WarnEvent> page = new Page<>(1, 100);
        page.setRecords(List.of());
        when(warnEventMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        WarnQueryDTO query = new WarnQueryDTO();
        query.setSize(9999);
        new WarnQueryServiceImpl(warnEventMapper).pageWarns(query);

        ArgumentCaptor<IPage<WarnEvent>> pageCaptor = ArgumentCaptor.forClass(IPage.class);
        verify(warnEventMapper).selectPage(pageCaptor.capture(), any(LambdaQueryWrapper.class));
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(100);
    }

    @Test
    void handleWarn_shouldUpdateStatusAndHandledBy() {
        when(warnEventMapper.selectById(1L)).thenReturn(sample());

        new WarnQueryServiceImpl(warnEventMapper).handleWarn(1L, 1, 999L);

        ArgumentCaptor<WarnEvent> captor = ArgumentCaptor.forClass(WarnEvent.class);
        verify(warnEventMapper).updateById(captor.capture());
        WarnEvent update = captor.getValue();
        assertThat(update.getId()).isEqualTo(1L);
        assertThat(update.getStatus()).isEqualTo(1);
        assertThat(update.getHandledBy()).isEqualTo(999L);
        assertThat(update.getHandledAt()).isNotNull();
    }

    @Test
    void handleWarn_shouldRejectIllegalStatus() {
        assertThatThrownBy(() -> new WarnQueryServiceImpl(warnEventMapper).handleWarn(1L, 9, 999L))
                .isInstanceOf(BizException.class);
        verify(warnEventMapper, never()).selectById(any());
        verify(warnEventMapper, never()).updateById(any(WarnEvent.class));
    }

    @Test
    void handleWarn_shouldThrowWhenNotFound() {
        when(warnEventMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> new WarnQueryServiceImpl(warnEventMapper).handleWarn(404L, 1, 999L))
                .isInstanceOf(BizException.class);
        verify(warnEventMapper, never()).updateById(any(WarnEvent.class));
    }
}
