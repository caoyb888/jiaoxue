package cn.smu.edu.jwxt.service.impl;

import cn.smu.edu.jwxt.domain.entity.JwxtIdMapping;
import cn.smu.edu.jwxt.repository.JwxtIdMappingMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwxtMappingServiceImplTest {

    @Mock
    JwxtIdMappingMapper idMappingMapper;

    @InjectMocks
    JwxtMappingServiceImpl service;

    @Test
    void resolveLocalId_shouldDelegateToBidirectionalLookup() {
        when(idMappingMapper.selectLocalIdByJwxtId("USER", "2021001")).thenReturn(88L);
        assertThat(service.resolveLocalId("USER", "2021001")).isEqualTo(88L);
        verify(idMappingMapper).selectLocalIdByJwxtId("USER", "2021001");
    }

    @Test
    void resolveJwxtId_shouldDelegateToReverseLookup() {
        when(idMappingMapper.selectJwxtIdByLocalId("USER", 88L)).thenReturn("2021001");
        assertThat(service.resolveJwxtId("USER", 88L)).isEqualTo("2021001");
        verify(idMappingMapper).selectJwxtIdByLocalId("USER", 88L);
    }

    @Test
    void saveMappings_shouldShortCircuitOnEmpty() {
        assertThat(service.saveMappings(List.of())).isZero();
        verify(idMappingMapper, never()).batchUpsert(anyList());
    }

    @Test
    void saveMappings_shouldDelegateBatchUpsert() {
        List<JwxtIdMapping> list = List.of(
                JwxtIdMapping.builder().dataType("DEPT").jwxtId("D1").localId(1L).build());
        when(idMappingMapper.batchUpsert(list)).thenReturn(1);
        assertThat(service.saveMappings(list)).isEqualTo(1);
    }

    @Test
    void upsert_shouldBuildSingleMappingAndDelegate() {
        when(idMappingMapper.batchUpsert(anyList())).thenReturn(1);

        service.upsert("COURSE", "C9", 33L, 500L);

        ArgumentCaptor<List<JwxtIdMapping>> captor = ArgumentCaptor.forClass(List.class);
        verify(idMappingMapper).batchUpsert(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        JwxtIdMapping m = captor.getValue().get(0);
        assertThat(m.getDataType()).isEqualTo("COURSE");
        assertThat(m.getJwxtId()).isEqualTo("C9");
        assertThat(m.getLocalId()).isEqualTo(33L);
        assertThat(m.getSyncLogId()).isEqualTo(500L);
    }
}
