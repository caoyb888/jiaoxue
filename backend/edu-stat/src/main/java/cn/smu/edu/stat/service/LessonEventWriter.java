package cn.smu.edu.stat.service;

import cn.smu.edu.stat.domain.entity.LessonEventLog;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ClickHouse {@code lesson_event_log} 批量写入器（削峰缓冲）。
 *
 * <p>架构约束（CLAUDE.md 7.4）：ClickHouse 不做单条 OLTP 写入，
 * 必须批量 INSERT（每批 ≥ 1000 条）。
 *
 * <ul>
 *   <li>事件先入内存队列；</li>
 *   <li>队列达到 {@link #BATCH_SIZE} 立即刷盘；</li>
 *   <li>{@link #scheduledFlush()} 每 5s 兜底刷盘，保证低峰期事件也能在 30s 内落库；</li>
 *   <li>关闭时 {@link #shutdownFlush()} 清空残留。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LessonEventWriter {

    /** 单批写入条数下限（架构约束 ≥ 1000）。 */
    static final int BATCH_SIZE = 1000;

    private static final String INSERT_SQL = """
            INSERT INTO lesson_event_log
                (stat_date, lesson_id, class_id, dept_id, teacher_id, event_type, student_id, event_value)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate clickHouseJdbcTemplate;

    private final ConcurrentLinkedQueue<LessonEventLog> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger pending = new AtomicInteger(0);
    private final ReentrantLock flushLock = new ReentrantLock();

    /** 事件入队；达到批量阈值时触发刷盘。线程安全（消费者 concurrency=5）。 */
    public void offer(LessonEventLog event) {
        queue.offer(event);
        if (pending.incrementAndGet() >= BATCH_SIZE) {
            flush();
        }
    }

    /** 每 5s 兜底刷盘，确保低峰期事件也能及时落库。 */
    @Scheduled(fixedDelay = 5000)
    public void scheduledFlush() {
        flush();
    }

    @PreDestroy
    public void shutdownFlush() {
        log.info("edu-stat 关闭，清空 lesson_event_log 缓冲队列，剩余={}", pending.get());
        flush();
    }

    /**
     * 将当前队列中的事件分批（每批 {@link #BATCH_SIZE}）写入 ClickHouse。
     * 用锁保证同一时刻只有一个刷盘线程，避免重复 drain。
     */
    private void flush() {
        if (pending.get() == 0 || !flushLock.tryLock()) {
            return;
        }
        try {
            // 先快照 drain，避免 writeBatch 失败重新入队后在同一循环里被立刻重读（死循环）
            List<LessonEventLog> drained = new ArrayList<>();
            LessonEventLog e;
            while ((e = queue.poll()) != null) {
                pending.decrementAndGet();
                drained.add(e);
            }
            for (int i = 0; i < drained.size(); i += BATCH_SIZE) {
                writeBatch(drained.subList(i, Math.min(i + BATCH_SIZE, drained.size())));
            }
        } finally {
            flushLock.unlock();
        }
    }

    private void writeBatch(List<LessonEventLog> batch) {
        try {
            clickHouseJdbcTemplate.batchUpdate(INSERT_SQL, batch, batch.size(), (ps, ev) -> {
                LocalDate statDate = ev.getStatDate() != null ? ev.getStatDate() : LocalDate.now();
                ps.setDate(1, Date.valueOf(statDate));
                ps.setLong(2, nz(ev.getLessonId()));
                ps.setLong(3, nz(ev.getClassId()));
                ps.setLong(4, nz(ev.getDeptId()));
                ps.setLong(5, nz(ev.getTeacherId()));
                ps.setString(6, ev.getEventType() == null ? "UNKNOWN" : ev.getEventType());
                ps.setLong(7, nz(ev.getStudentId()));
                ps.setString(8, ev.getEventValue() == null ? "" : ev.getEventValue());
            });
            log.info("ClickHouse lesson_event_log 批量写入完成: count={}", batch.size());
        } catch (Exception ex) {
            // 写入失败：事件重新入队，等待下次刷盘重试（避免丢数据）
            queue.addAll(batch);
            pending.addAndGet(batch.size());
            log.error("ClickHouse 批量写入失败，已重新入队 count={}", batch.size(), ex);
        }
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }
}
