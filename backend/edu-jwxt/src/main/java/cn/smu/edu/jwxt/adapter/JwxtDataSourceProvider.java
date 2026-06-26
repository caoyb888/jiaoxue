package cn.smu.edu.jwxt.adapter;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 按配置 {@code jwxt.vendor}（zhengfang/qiangzhi/stub，dev 默认 stub）选择当前生效的数据源适配器。
 */
@Slf4j
@Component
public class JwxtDataSourceProvider {

    private final Map<String, JwxtDataSource> byVendor;

    @Value("${jwxt.vendor:stub}")
    private String vendor;

    public JwxtDataSourceProvider(List<JwxtDataSource> dataSources) {
        this.byVendor = dataSources.stream()
                .collect(Collectors.toMap(JwxtDataSource::vendor, Function.identity()));
    }

    @PostConstruct
    void logActive() {
        log.info("jwxt 数据源适配层就绪: 可用={}, 当前生效={}", byVendor.keySet(), vendor);
    }

    /** 当前生效数据源；配置厂商不存在时抛错（防止静默用错数据源）。 */
    public JwxtDataSource active() {
        JwxtDataSource ds = byVendor.get(vendor);
        if (ds == null) {
            throw new IllegalStateException("未找到 jwxt.vendor=" + vendor + " 的数据源适配器，可用: " + byVendor.keySet());
        }
        return ds;
    }
}
