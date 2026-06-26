package cn.smu.edu.jwxt.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 强智教务系统适配器。主键字段沿用强智习惯命名（学生编号 xsbh、部门编号 bmbh、课程编号 kcbh、教学班编号 jxbbh）。
 */
@Component
public class QiangzhiAdapter extends AbstractJwxtAdapter {

    private static final Map<String, String> ID_FIELDS = Map.of(
            JwxtDataType.STUDENT, "xsbh",
            JwxtDataType.DEPT, "bmbh",
            JwxtDataType.COURSE, "kcbh",
            JwxtDataType.CLASS, "jxbbh");

    @Value("${jwxt.qiangzhi.base-url:}")
    private String baseUrl;

    public QiangzhiAdapter(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public String vendor() {
        return "qiangzhi";
    }

    @Override
    protected String idField(String dataType) {
        String field = ID_FIELDS.get(dataType);
        if (field == null) {
            throw new IllegalArgumentException("强智适配器不支持的数据类型: " + dataType);
        }
        return field;
    }

    @Override
    protected String baseUrl() {
        return baseUrl;
    }
}
