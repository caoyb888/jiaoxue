package cn.smu.edu.stat.service.impl;

import cn.smu.edu.stat.domain.vo.ClassDailyStatVO;
import cn.smu.edu.stat.domain.vo.ClassHistoryVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryStatServiceImplTest {

    @Mock
    JdbcTemplate clickHouseJdbcTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void classHistory_shouldQueryWithStatDatePartitionRangeAndMapRows() {
        ClassDailyStatVO row = new ClassDailyStatVO("2026-06-25", 2, 50, 30, 5, 3, 12, 48);
        when(clickHouseJdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of(row));

        HistoryStatServiceImpl service = new HistoryStatServiceImpl(clickHouseJdbcTemplate);
        ClassHistoryVO vo = service.classHistory(99L, 7);

        assertThat(vo.classId()).isEqualTo(99L);
        assertThat(vo.toDate()).isEqualTo(LocalDate.now().toString());
        assertThat(vo.fromDate()).isEqualTo(LocalDate.now().minusDays(6).toString());
        assertThat(vo.daily()).containsExactly(row);

        // SQL 必须以 stat_date 分区键作为 WHERE 第一约束（CLAUDE.md §7.4）
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(clickHouseJdbcTemplate).query(sqlCaptor.capture(), any(PreparedStatementSetter.class), any(RowMapper.class));
        String sql = sqlCaptor.getValue();
        assertThat(sql).contains("WHERE stat_date >= ? AND stat_date <= ? AND class_id = ?");
        assertThat(sql).contains("GROUP BY stat_date");
    }

    @Test
    @SuppressWarnings("unchecked")
    void classHistory_shouldBindFromToAndClassIdParams() throws Exception {
        when(clickHouseJdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of());

        HistoryStatServiceImpl service = new HistoryStatServiceImpl(clickHouseJdbcTemplate);
        service.classHistory(42L, 30);

        ArgumentCaptor<PreparedStatementSetter> setterCaptor = ArgumentCaptor.forClass(PreparedStatementSetter.class);
        verify(clickHouseJdbcTemplate).query(anyString(), setterCaptor.capture(), any(RowMapper.class));

        PreparedStatement ps = org.mockito.Mockito.mock(PreparedStatement.class);
        setterCaptor.getValue().setValues(ps);

        LocalDate today = LocalDate.now();
        verify(ps).setDate(1, Date.valueOf(today.minusDays(29)));
        verify(ps).setDate(2, Date.valueOf(today));
        verify(ps).setLong(3, 42L);
    }
}
