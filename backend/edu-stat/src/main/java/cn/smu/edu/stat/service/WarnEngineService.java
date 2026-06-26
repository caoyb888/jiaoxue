package cn.smu.edu.stat.service;

import java.time.LocalDate;

/**
 * 教学预警引擎（S7-06）——从 ClickHouse 明细计算三类预警，去重写入 MySQL {@code warn_event}。
 *
 * <ul>
 *   <li>LOW_ATTEND       低考勤：课堂去重签到学生数 &gt; 0 但低于阈值；</li>
 *   <li>ZERO_ACTIVE      零活跃：有签到但无弹幕/提问/加分/翻页等互动；</li>
 *   <li>FREQUENT_ABSENCE 频繁缺席：学生近 N 天在其班级出勤课次占比过低。</li>
 * </ul>
 */
public interface WarnEngineService {

    /**
     * 执行一次预警检查。
     *
     * @param statDate 统计日期（课堂级预警针对当天；缺席预警回溯窗口截止当天）
     * @return 本次去重写入的预警条数（upsert 受影响行数）
     */
    int runCheck(LocalDate statDate);
}
