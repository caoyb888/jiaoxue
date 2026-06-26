package cn.smu.edu.jwxt.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwxtDataSourceProviderTest {

    private JwxtDataSourceProvider provider(String vendor) {
        ObjectMapper om = new ObjectMapper();
        JwxtDataSourceProvider p = new JwxtDataSourceProvider(
                List.of(new StubJwxtDataSource(), new ZhengfangAdapter(om), new QiangzhiAdapter(om)));
        ReflectionTestUtils.setField(p, "vendor", vendor);
        return p;
    }

    @Test
    void active_shouldSelectByVendorConfig() {
        assertThat(provider("stub").active().vendor()).isEqualTo("stub");
        assertThat(provider("zhengfang").active().vendor()).isEqualTo("zhengfang");
        assertThat(provider("qiangzhi").active().vendor()).isEqualTo("qiangzhi");
    }

    @Test
    void active_shouldThrowWhenVendorUnknown() {
        assertThatThrownBy(() -> provider("unknown").active())
                .isInstanceOf(IllegalStateException.class);
    }
}
