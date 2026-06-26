package cn.smu.edu.grade.export;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 按 {@code format} 选择成绩回传模板（zhengfang/qiangzhi）。
 */
@Component
public class GradeExportTemplateProvider {

    private final Map<String, GradeExportTemplate> byFormat;

    public GradeExportTemplateProvider(List<GradeExportTemplate> templates) {
        this.byFormat = templates.stream()
                .collect(Collectors.toMap(GradeExportTemplate::format, Function.identity()));
    }

    /** 取指定格式模板；不支持的 format 抛参数异常（含可用列表）。 */
    public GradeExportTemplate resolve(String format) {
        GradeExportTemplate template = byFormat.get(format);
        if (template == null) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(),
                    "不支持的导出格式: " + format + "，可用: " + byFormat.keySet());
        }
        return template;
    }
}
