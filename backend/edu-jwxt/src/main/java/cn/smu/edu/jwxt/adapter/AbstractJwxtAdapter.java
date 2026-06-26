package cn.smu.edu.jwxt.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 厂商适配器公共逻辑。
 *
 * <p>真实 HTTP 接入留待 S7-16 联调（需学校教务沙盒 API）：未配置 {@link #baseUrl()} 时
 * {@link #fetchIncremental} 返回空，dev 环境用 {@link StubJwxtDataSource} 提供样例数据。
 * 各厂商差异集中在 {@link #idField(String)}（教务主键字段名），归一逻辑 {@link #toRecord}
 * 复用并被单测覆盖。
 */
@Slf4j
public abstract class AbstractJwxtAdapter implements JwxtDataSource {

    protected final ObjectMapper objectMapper;

    protected AbstractJwxtAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 某数据类型在该厂商响应中的主键字段名。 */
    protected abstract String idField(String dataType);

    /** 厂商教务接口基地址；空表示未接入真实接口。 */
    protected abstract String baseUrl();

    @Override
    public List<JwxtRecord> fetchIncremental(String dataType, LocalDate since, int offset, int limit) {
        if (!StringUtils.hasText(baseUrl())) {
            log.debug("[{}] 教务接口未配置 baseUrl，跳过拉取 dataType={}（真实接入见 S7-16）", vendor(), dataType);
            return List.of();
        }
        // 真实环境：HTTP 调厂商接口 → 逐行 toRecord。沙盒 API 就绪后在 S7-16 接入。
        log.warn("[{}] baseUrl 已配置但 HTTP 拉取尚未接入，返回空 dataType={}", vendor(), dataType);
        return List.of();
    }

    /**
     * 厂商响应行 → 归一记录。按 {@link #idField(String)} 取教务主键，整行序列化为 JSON。
     *
     * @throws IllegalArgumentException 行内缺少主键字段
     */
    public JwxtRecord toRecord(String dataType, Map<String, Object> row) {
        Object id = row.get(idField(dataType));
        if (id == null || !StringUtils.hasText(String.valueOf(id))) {
            throw new IllegalArgumentException("教务记录缺少主键字段 " + idField(dataType) + " (dataType=" + dataType + ")");
        }
        try {
            return new JwxtRecord(dataType, String.valueOf(id), objectMapper.writeValueAsString(row));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化教务原始数据失败", e);
        }
    }
}
