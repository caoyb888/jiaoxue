package cn.smu.edu.jwxt.service.impl;

import cn.smu.edu.jwxt.adapter.JwxtDataSource;
import cn.smu.edu.jwxt.adapter.JwxtDataSourceProvider;
import cn.smu.edu.jwxt.adapter.JwxtDataType;
import cn.smu.edu.jwxt.adapter.JwxtRecord;
import cn.smu.edu.jwxt.domain.entity.JwxtRawData;
import cn.smu.edu.jwxt.domain.entity.JwxtSyncLog;
import cn.smu.edu.jwxt.repository.JwxtSyncLogMapper;
import cn.smu.edu.jwxt.service.JwxtIncrementalSyncService;
import cn.smu.edu.jwxt.service.JwxtMappingService;
import cn.smu.edu.jwxt.service.JwxtRawDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * {@link JwxtIncrementalSyncService} 实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwxtIncrementalSyncServiceImpl implements JwxtIncrementalSyncService {

    private static final String SYNC_TYPE = "INCREMENTAL";
    private static final int STATUS_RUNNING = 0;
    private static final int STATUS_SUCCESS = 1;
    private static final int STATUS_FAILED = 3;
    /** 无历史成功同步时的默认增量基准回溯天数。 */
    private static final int DEFAULT_LOOKBACK_DAYS = 7;

    @Value("${jwxt.sync.batch-size:500}")
    private int batchSize;

    private final JwxtDataSourceProvider dataSourceProvider;
    private final JwxtRawDataService rawDataService;
    private final JwxtMappingService mappingService;
    private final JwxtSyncLogMapper syncLogMapper;

    @Override
    public long incrementalSync(String triggeredBy) {
        long start = System.currentTimeMillis();
        LocalDate since = lastSuccessfulSyncDate();

        JwxtSyncLog syncLog = JwxtSyncLog.builder()
                .syncType(SYNC_TYPE).syncDate(LocalDate.now()).status(STATUS_RUNNING)
                .studentCnt(0).deptCnt(0).courseCnt(0).successCnt(0).failedCnt(0)
                .triggeredBy(triggeredBy == null ? "SCHEDULE" : triggeredBy)
                .build();
        syncLogMapper.insert(syncLog);
        long logId = syncLog.getId();
        log.info("教务增量同步开始: logId={}, since={}, vendor={}, batchSize={}",
                logId, since, dataSourceProvider.active().vendor(), batchSize);

        try {
            JwxtDataSource ds = dataSourceProvider.active();
            int student = 0, dept = 0, course = 0, total = 0;
            for (String dataType : JwxtDataType.SYNC_ORDER) {
                int typeCount = syncType(ds, dataType, since, logId);
                total += typeCount;
                switch (dataType) {
                    case JwxtDataType.STUDENT -> student += typeCount;
                    case JwxtDataType.DEPT -> dept += typeCount;
                    default -> course += typeCount; // COURSE + CLASS 合并计入 course_cnt
                }
            }

            JwxtSyncLog done = new JwxtSyncLog();
            done.setId(logId);
            done.setStudentCnt(student);
            done.setDeptCnt(dept);
            done.setCourseCnt(course);
            done.setSuccessCnt(total);
            done.setStatus(STATUS_SUCCESS);
            done.setCostMs(System.currentTimeMillis() - start);
            done.setFinishedAt(LocalDateTime.now());
            syncLogMapper.updateById(done);
            log.info("教务增量同步完成: logId={}, 学生={}, 院系={}, 课程/班={}, 合计={}, 耗时={}ms",
                    logId, student, dept, course, total, done.getCostMs());
            return logId;
        } catch (Exception e) {
            JwxtSyncLog failed = new JwxtSyncLog();
            failed.setId(logId);
            failed.setStatus(STATUS_FAILED);
            failed.setErrorMsg(truncate(e.getMessage()));
            failed.setCostMs(System.currentTimeMillis() - start);
            failed.setFinishedAt(LocalDateTime.now());
            syncLogMapper.updateById(failed);
            log.error("教务增量同步失败: logId={}", logId, e);
            throw e;
        }
    }

    /** 分批拉取某一数据类型，暂存原始数据并按映射对照统计新增/更新，返回该类型处理条数。 */
    private int syncType(JwxtDataSource ds, String dataType, LocalDate since, long logId) {
        String mappingType = mappingType(dataType);
        int offset = 0, count = 0, added = 0, updated = 0;
        while (true) {
            List<JwxtRecord> batch = ds.fetchIncremental(dataType, since, offset, batchSize);
            if (batch.isEmpty()) {
                break;
            }
            List<JwxtRawData> rows = batch.stream()
                    .map(r -> JwxtRawData.builder()
                            .syncLogId(logId).dataType(r.dataType()).jwxtId(r.jwxtId())
                            .rawJson(r.rawJson()).status(0).build())
                    .toList();
            rawDataService.stage(rows);

            for (JwxtRecord r : batch) {
                if (mappingService.resolveLocalId(mappingType, r.jwxtId()) != null) {
                    updated++;
                } else {
                    added++;
                }
            }
            count += batch.size();
            offset += batch.size();
            if (batch.size() < batchSize) {
                break; // 不足一批，已到末页
            }
        }
        log.info("增量同步 dataType={}: 处理={}, 新增={}, 更新={}", dataType, count, added, updated);
        return count;
    }

    /** raw_data 的 STUDENT 对应 id_mapping 的 USER；其余类型同名。 */
    private static String mappingType(String dataType) {
        return JwxtDataType.STUDENT.equals(dataType) ? "USER" : dataType;
    }

    /** 上次成功同步基准日期；无则回溯默认天数。 */
    private LocalDate lastSuccessfulSyncDate() {
        Page<JwxtSyncLog> page = syncLogMapper.selectPage(new Page<>(1, 1),
                new LambdaQueryWrapper<JwxtSyncLog>()
                        .eq(JwxtSyncLog::getStatus, STATUS_SUCCESS)
                        .orderByDesc(JwxtSyncLog::getSyncDate)
                        .orderByDesc(JwxtSyncLog::getId));
        List<JwxtSyncLog> records = page.getRecords();
        return records.isEmpty()
                ? LocalDate.now().minusDays(DEFAULT_LOOKBACK_DAYS)
                : records.get(0).getSyncDate();
    }

    private static String truncate(String msg) {
        if (msg == null) {
            return "未知错误";
        }
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}
