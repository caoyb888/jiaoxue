package cn.smu.edu.jwxt.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 正方教务系统适配器。主键字段沿用正方习惯命名（学号 xh、单位号 dwh、课程号 kch、教学班 jxbid）。
 */
@Component
public class ZhengfangAdapter extends AbstractJwxtAdapter {

    private static final Map<String, String> ID_FIELDS = Map.of(
            JwxtDataType.STUDENT, "xh",
            JwxtDataType.DEPT, "dwh",
            JwxtDataType.COURSE, "kch",
            JwxtDataType.CLASS, "jxbid");

    @Value("${jwxt.zhengfang.base-url:}")
    private String baseUrl;

    public ZhengfangAdapter(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public String vendor() {
        return "zhengfang";
    }

    @Override
    protected String idField(String dataType) {
        String field = ID_FIELDS.get(dataType);
        if (field == null) {
            throw new IllegalArgumentException("正方适配器不支持的数据类型: " + dataType);
        }
        return field;
    }

    @Override
    protected String baseUrl() {
        return baseUrl;
    }
}
