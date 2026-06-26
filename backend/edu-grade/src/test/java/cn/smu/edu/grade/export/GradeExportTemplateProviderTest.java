package cn.smu.edu.grade.export;

import cn.smu.edu.common.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GradeExportTemplateProviderTest {

    private final GradeExportTemplateProvider provider = new GradeExportTemplateProvider(
            List.of(new ZhengfangGradeTemplate(), new QiangzhiGradeTemplate()));

    @Test
    void resolve_shouldReturnTemplateByFormat() {
        assertThat(provider.resolve("zhengfang").format()).isEqualTo("zhengfang");
        assertThat(provider.resolve("qiangzhi").format()).isEqualTo("qiangzhi");
    }

    @Test
    void resolve_shouldThrowOnUnsupportedFormat() {
        assertThatThrownBy(() -> provider.resolve("excel99"))
                .isInstanceOf(BizException.class);
    }
}
