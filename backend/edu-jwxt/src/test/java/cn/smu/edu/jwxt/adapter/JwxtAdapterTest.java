package cn.smu.edu.jwxt.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwxtAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void zhengfang_toRecord_shouldUseVendorSpecificIdField() {
        ZhengfangAdapter adapter = new ZhengfangAdapter(objectMapper);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("xh", "2021001");
        row.put("xm", "张三");

        JwxtRecord record = adapter.toRecord(JwxtDataType.STUDENT, row);

        assertThat(adapter.vendor()).isEqualTo("zhengfang");
        assertThat(record.dataType()).isEqualTo("STUDENT");
        assertThat(record.jwxtId()).isEqualTo("2021001");
        assertThat(record.rawJson()).contains("\"xh\":\"2021001\"").contains("张三");
    }

    @Test
    void qiangzhi_toRecord_shouldUseDifferentIdField() {
        QiangzhiAdapter adapter = new QiangzhiAdapter(objectMapper);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("xsbh", "S2021001");
        row.put("xm", "李四");

        JwxtRecord record = adapter.toRecord(JwxtDataType.STUDENT, row);

        assertThat(adapter.vendor()).isEqualTo("qiangzhi");
        assertThat(record.jwxtId()).isEqualTo("S2021001");
    }

    @Test
    void toRecord_shouldThrowWhenIdFieldMissing() {
        ZhengfangAdapter adapter = new ZhengfangAdapter(objectMapper);
        assertThatThrownBy(() -> adapter.toRecord(JwxtDataType.STUDENT, Map.of("xm", "无学号")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fetchIncremental_shouldReturnEmptyWhenBaseUrlUnset() {
        ZhengfangAdapter adapter = new ZhengfangAdapter(objectMapper);
        assertThat(adapter.fetchIncremental(JwxtDataType.STUDENT, java.time.LocalDate.now(), 0, 500))
                .isEmpty();
    }
}
