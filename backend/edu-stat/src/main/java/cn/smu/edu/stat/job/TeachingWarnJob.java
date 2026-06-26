package cn.smu.edu.stat.job;

import cn.smu.edu.stat.service.WarnEngineService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * 教学预警引擎定时任务（S7-06），建议每日 23:30 在 18160 admin 配置 cron 触发。
 *
 * <p>支持任务参数指定统计日期（yyyy-MM-dd，便于补算历史）；空参默认当天。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeachingWarnJob {

    private final WarnEngineService warnEngineService;

    @XxlJob("teachingWarnCheck")
    public void teachingWarnCheck() {
        LocalDate statDate = parseDateParam(XxlJobHelper.getJobParam());
        log.info("开始执行教学预警检查: statDate={}", statDate);
        int count = warnEngineService.runCheck(statDate);
        XxlJobHelper.handleSuccess("教学预警检查完成，命中 " + count + " 条");
    }

    /** 解析任务参数为日期；空白或非法格式回退当天。 */
    private static LocalDate parseDateParam(String param) {
        if (param == null || param.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(param.trim());
        } catch (DateTimeParseException e) {
            log.warn("teachingWarnCheck 任务参数非法日期 '{}'，回退当天", param);
            return LocalDate.now();
        }
    }
}
