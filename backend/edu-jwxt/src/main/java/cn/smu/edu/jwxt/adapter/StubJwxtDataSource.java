package cn.smu.edu.jwxt.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 开发/演示用桩数据源（vendor=stub，dev 默认）。
 *
 * <p>真实正方/强智沙盒 API 未就绪前，让增量同步链路（拉取→暂存→映射对照→日志）
 * 在 dev 端可端到端跑通。每类返回少量确定性样例，第二页起为空（模拟有限数据）。
 */
@Slf4j
@Component
public class StubJwxtDataSource implements JwxtDataSource {

    private static final int STUB_COUNT = 2;

    @Override
    public String vendor() {
        return "stub";
    }

    @Override
    public List<JwxtRecord> fetchIncremental(String dataType, LocalDate since, int offset, int limit) {
        if (offset > 0) {
            return List.of();
        }
        List<JwxtRecord> records = new ArrayList<>(STUB_COUNT);
        for (int i = 1; i <= STUB_COUNT; i++) {
            String jwxtId = dataType + "-" + i;
            String rawJson = "{\"dataType\":\"" + dataType + "\",\"jwxtId\":\"" + jwxtId
                    + "\",\"since\":\"" + since + "\"}";
            records.add(new JwxtRecord(dataType, jwxtId, rawJson));
        }
        log.debug("[stub] 返回 {} 条样例 dataType={}", records.size(), dataType);
        return records;
    }
}
