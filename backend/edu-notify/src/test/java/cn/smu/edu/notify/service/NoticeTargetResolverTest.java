package cn.smu.edu.notify.service;

import cn.smu.edu.notify.domain.entity.Notice;
import cn.smu.edu.notify.repository.NoticeTargetMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeTargetResolverTest {

    @Mock
    NoticeTargetMapper targetMapper;
    @InjectMocks
    NoticeTargetResolver resolver;

    @Test
    void resolve_school_all_shouldQueryWithoutFilters() {
        Notice n = Notice.builder().scope("SCHOOL").targetRoles("ALL").build();
        when(targetMapper.selectUserIds(null, null)).thenReturn(List.of(1L, 2L));

        assertThat(resolver.resolve(n)).containsExactly(1L, 2L);
        verify(targetMapper).selectUserIds(null, null);
    }

    @Test
    void resolve_school_teacher_shouldFilterUserType2() {
        Notice n = Notice.builder().scope("SCHOOL").targetRoles("TEACHER").build();
        when(targetMapper.selectUserIds(null, 2)).thenReturn(List.of(5L));

        assertThat(resolver.resolve(n)).containsExactly(5L);
    }

    @Test
    void resolve_dept_student_shouldFilterDeptAndType1() {
        Notice n = Notice.builder().scope("DEPT").deptId(10L).targetRoles("STUDENT").build();
        when(targetMapper.selectUserIds(10L, 1)).thenReturn(List.of(8L, 9L));

        assertThat(resolver.resolve(n)).containsExactly(8L, 9L);
        verify(targetMapper).selectUserIds(10L, 1);
    }

    @Test
    void resolve_class_shouldQueryClassStudents() {
        Notice n = Notice.builder().scope("CLASS").classId(3L).build();
        when(targetMapper.selectClassStudentIds(3L)).thenReturn(List.of(100L));

        assertThat(resolver.resolve(n)).containsExactly(100L);
    }

    @Test
    void resolve_unknownScope_shouldReturnEmpty() {
        Notice n = Notice.builder().scope("X").build();
        assertThat(resolver.resolve(n)).isEmpty();
    }
}
