package cn.smu.edu.stat.service.impl;

import cn.smu.edu.stat.domain.entity.WarnEvent;
import cn.smu.edu.stat.repository.WarnEventMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarnEngineServiceImplTest {

    @Mock
    JdbcTemplate clickHouseJdbcTemplate;
    @Mock
    WarnEventMapper warnEventMapper;

    /** 把指定 ResultSet 喂给匹配 SQL 标记的那次 query 的 RowCallbackHandler。 */
    private void stubQuery(String sqlMarker, ResultSet rs) {
        doAnswer(inv -> {
            RowCallbackHandler rch = inv.getArgument(2);
            rch.processRow(rs);
            return null;
        }).when(clickHouseJdbcTemplate).query(
                argThat((String sql) -> sql.contains(sqlMarker)),
                any(PreparedStatementSetter.class),
                any(RowCallbackHandler.class));
    }

    @Test
    void runCheck_shouldDetectLowAttendZeroActiveAndFrequentAbsence_andUpsert() throws Exception {
        // 课堂级：签到 5 人(<15→LOW_ATTEND) 且零互动(active=0→ZERO_ACTIVE)
        ResultSet lessonRs = mock(ResultSet.class);
        when(lessonRs.getLong("lesson_id")).thenReturn(1001L);
        when(lessonRs.getLong("class_id")).thenReturn(100L);
        when(lessonRs.getLong("dept_id")).thenReturn(10L);
        when(lessonRs.getLong("teacher_id")).thenReturn(50L);
        when(lessonRs.getInt("attend_students")).thenReturn(5);
        when(lessonRs.getLong("active_count")).thenReturn(0L);
        stubQuery("attend_students", lessonRs);

        // 班级窗口总课次 5
        ResultSet classRs = mock(ResultSet.class);
        when(classRs.getLong("class_id")).thenReturn(100L);
        when(classRs.getInt("total_lessons")).thenReturn(5);
        stubQuery("AS total_lessons", classRs);

        // 学生窗口出勤 2 次(<ceil(5*0.6)=3→FREQUENT_ABSENCE)
        ResultSet studentRs = mock(ResultSet.class);
        when(studentRs.getLong("class_id")).thenReturn(100L);
        when(studentRs.getLong("student_id")).thenReturn(200L);
        when(studentRs.getLong("dept_id")).thenReturn(10L);
        when(studentRs.getInt("attended_lessons")).thenReturn(2);
        stubQuery("attended_lessons", studentRs);

        when(warnEventMapper.upsertBatch(anyList())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());

        WarnEngineServiceImpl service = new WarnEngineServiceImpl(clickHouseJdbcTemplate, warnEventMapper);
        int affected = service.runCheck(LocalDate.of(2026, 6, 26));

        assertThat(affected).isEqualTo(3);

        ArgumentCaptor<List<WarnEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(warnEventMapper).upsertBatch(captor.capture());
        List<WarnEvent> warnings = captor.getValue();
        assertThat(warnings).extracting(WarnEvent::getWarnType)
                .containsExactlyInAnyOrder("LOW_ATTEND", "ZERO_ACTIVE", "FREQUENT_ABSENCE");

        WarnEvent absence = warnings.stream()
                .filter(w -> "FREQUENT_ABSENCE".equals(w.getWarnType())).findFirst().orElseThrow();
        assertThat(absence.getTargetType()).isEqualTo("STUDENT");
        assertThat(absence.getTargetId()).isEqualTo(200L);
        assertThat(absence.getMetricValue()).isEqualTo(2);
        assertThat(absence.getThresholdValue()).isEqualTo(3);
    }

    @Test
    void runCheck_shouldSkipLessonWithNoAttendance_andNotUpsertWhenEmpty() throws Exception {
        // 课堂无人签到 → 不评估低考勤/零活跃
        ResultSet lessonRs = mock(ResultSet.class);
        when(lessonRs.getInt("attend_students")).thenReturn(0);
        stubQuery("attend_students", lessonRs);
        // 缺席查询：班级课次不足 ABSENCE_MIN_LESSONS（2 < 3）→ 跳过
        ResultSet classRs = mock(ResultSet.class);
        when(classRs.getLong("class_id")).thenReturn(100L);
        when(classRs.getInt("total_lessons")).thenReturn(2);
        stubQuery("AS total_lessons", classRs);
        ResultSet studentRs = mock(ResultSet.class);
        when(studentRs.getLong("class_id")).thenReturn(100L);
        when(studentRs.getLong("student_id")).thenReturn(200L);
        when(studentRs.getInt("attended_lessons")).thenReturn(0);
        stubQuery("attended_lessons", studentRs);

        WarnEngineServiceImpl service = new WarnEngineServiceImpl(clickHouseJdbcTemplate, warnEventMapper);
        int affected = service.runCheck(LocalDate.of(2026, 6, 26));

        assertThat(affected).isZero();
        verify(warnEventMapper, org.mockito.Mockito.never()).upsertBatch(anyList());
    }
}
