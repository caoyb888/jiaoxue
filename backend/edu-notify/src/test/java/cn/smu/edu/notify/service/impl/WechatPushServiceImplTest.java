package cn.smu.edu.notify.service.impl;

import cn.smu.edu.notify.domain.entity.Notice;
import cn.smu.edu.notify.repository.NoticeTargetMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WechatPushServiceImplTest {

    @Mock
    NoticeTargetMapper targetMapper;
    @InjectMocks
    WechatPushServiceImpl service;

    private Notice notice() {
        return Notice.builder().id(1L).title("通知").build();
    }

    @Test
    void pushNotice_shouldChunkBy500AndCountBoundOpenIds() {
        ReflectionTestUtils.setField(service, "mockMode", true);
        // 1200 个用户 → 3 块（500/500/200）
        List<Long> userIds = LongStream.rangeClosed(1, 1200).boxed().collect(Collectors.toList());
        // 每块返回与块等量的 openId（全部已绑定微信）
        when(targetMapper.selectOpenIdsByUserIds(anyList()))
                .thenAnswer(inv -> inv.getArgument(0, List.class).stream()
                        .map(x -> "openid-" + x).collect(Collectors.toList()));

        int pushed = service.pushNotice(notice(), userIds);

        assertThat(pushed).isEqualTo(1200);
        verify(targetMapper, times(3)).selectOpenIdsByUserIds(anyList());
    }

    @Test
    void pushNotice_shouldSkipChunksWithNoWechatBinding() {
        ReflectionTestUtils.setField(service, "mockMode", true);
        when(targetMapper.selectOpenIdsByUserIds(anyList())).thenReturn(List.of());

        int pushed = service.pushNotice(notice(), List.of(1L, 2L, 3L));

        assertThat(pushed).isZero();
    }

    @Test
    void pushNotice_emptyTargets_shouldReturnZeroWithoutQuery() {
        int pushed = service.pushNotice(notice(), List.of());

        assertThat(pushed).isZero();
        verify(targetMapper, never()).selectOpenIdsByUserIds(anyList());
    }
}
