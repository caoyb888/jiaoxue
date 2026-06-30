package cn.smu.edu.grade.job;

import cn.smu.edu.grade.service.GradeCalcService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 综合成绩计算定时任务（S8-08）。
 *
 * <p>任务参数为教学班 ID 时只重算该班；空参则计算所有 calc_status=0 的待算班级。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GradeCalcJob {

    private final GradeCalcService gradeCalcService;

    @XxlJob("gradeCalc")
    public void gradeCalc() {
        String param = XxlJobHelper.getJobParam();
        int count;
        if (param != null && param.trim().matches("\\d+")) {
            Long classId = Long.valueOf(param.trim());
            count = gradeCalcService.calculateClass(classId);
            XxlJobHelper.handleSuccess("班级 " + classId + " 成绩计算完成，学生数 " + count);
        } else {
            count = gradeCalcService.calculatePending();
            XxlJobHelper.handleSuccess("待计算成绩处理完成，学生数 " + count);
        }
    }
}
