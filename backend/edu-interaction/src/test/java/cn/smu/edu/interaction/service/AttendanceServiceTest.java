package cn.smu.edu.interaction.service;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.interaction.domain.dto.AttendDTO;
import cn.smu.edu.interaction.domain.entity.Attendance;
import cn.smu.edu.interaction.domain.vo.AttendResultVO;
import cn.smu.edu.interaction.repository.AttendanceCodeMapper;
import cn.smu.edu.interaction.repository.AttendanceMapper;
import cn.smu.edu.interaction.service.impl.AttendanceServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock private AttendanceCodeMapper attendanceCodeMapper;
    @Mock private AttendanceMapper attendanceMapper;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private RedissonClient redissonClient;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private ListOperations<String, String> listOps;
    @SuppressWarnings("rawtypes")
    @Mock private RBloomFilter bloomFilter;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks private AttendanceServiceImpl attendanceService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setup() throws Exception {
        // 注入 objectMapper（@InjectMocks 不自动注入非 Mock 字段）
        var field = AttendanceServiceImpl.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(attendanceService, objectMapper);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.opsForList()).thenReturn(listOps);
        lenient().when(redissonClient.<String>getBloomFilter(anyString())).thenReturn(bloomFilter);
    }

    @Test
    void attend_shouldThrow_whenCodeInvalid() {
        when(valueOps.get(anyString())).thenReturn(null);

        AttendDTO dto = new AttendDTO();
        dto.setCode("ABCDEF");

        assertThrows(BizException.class, () ->
                attendanceService.attend(1L, 100L, dto));
    }

    @Test
    void attend_shouldReturnAlreadyAttended_whenBloomFilterHit() {
        when(valueOps.get(anyString())).thenReturn("ABCDEF|qrtoken123");
        when(bloomFilter.isExists()).thenReturn(true);
        when(bloomFilter.contains("100")).thenReturn(true);
        when(valueOps.get(contains("count"))).thenReturn("15");

        AttendDTO dto = new AttendDTO();
        dto.setCode("ABCDEF");

        AttendResultVO result = attendanceService.attend(1L, 100L, dto);

        assertFalse(result.isFirstAttend());
        assertEquals("您已签到，请勿重复操作", result.getMessage());
        verify(listOps, never()).rightPush(anyString(), anyString());
    }

    @Test
    void attend_shouldPushToQueue_whenFirstAttend() {
        when(valueOps.get(contains("code"))).thenReturn("ABCDEF|qrtoken123");
        when(bloomFilter.isExists()).thenReturn(true);
        when(bloomFilter.contains("100")).thenReturn(false);
        when(valueOps.increment(anyString())).thenReturn(1L);

        AttendDTO dto = new AttendDTO();
        dto.setCode("ABCDEF");

        AttendResultVO result = attendanceService.attend(1L, 100L, dto);

        assertTrue(result.isFirstAttend());
        assertEquals(1L, result.getTotalCount());
        verify(bloomFilter).add("100");
        verify(listOps).rightPush(anyString(), anyString());
    }

    @Test
    void attend_shouldAcceptQrToken() {
        when(valueOps.get(contains("code"))).thenReturn("ABCDEF|qrtoken123");
        when(bloomFilter.isExists()).thenReturn(true);
        when(bloomFilter.contains("200")).thenReturn(false);
        when(valueOps.increment(anyString())).thenReturn(5L);

        AttendDTO dto = new AttendDTO();
        dto.setQrToken("qrtoken123");

        AttendResultVO result = attendanceService.attend(1L, 200L, dto);

        assertTrue(result.isFirstAttend());
        verify(listOps).rightPush(anyString(), anyString());
    }
}
