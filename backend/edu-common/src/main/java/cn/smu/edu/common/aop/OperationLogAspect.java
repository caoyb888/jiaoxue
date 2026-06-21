package cn.smu.edu.common.aop;

import cn.smu.edu.common.util.IpUtil;
import cn.smu.edu.common.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 操作日志 AOP — 等保三级要求，留存 180 天，写入 edu_audit_db.log_operation
 * 实际落库通过 Kafka 异步写入，避免阻塞主流程
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class OperationLogAspect {

    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint pjp, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        String status = "SUCCESS";
        String errorMsg = null;

        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable e) {
            status = "FAIL";
            errorMsg = e.getMessage();
            throw e;
        } finally {
            try {
                sendLog(operationLog, pjp, status, errorMsg, System.currentTimeMillis() - start);
            } catch (Exception ex) {
                log.warn("操作日志发送失败，不影响主流程: {}", ex.getMessage());
            }
        }
    }

    private void sendLog(OperationLog annotation, ProceedingJoinPoint pjp,
                         String status, String errorMsg, long costMs) {
        HttpServletRequest request = null;
        try {
            request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        } catch (Exception ignored) {
        }

        OperationLogEvent event = new OperationLogEvent();
        event.setLogId(UUID.randomUUID().toString().replace("-", ""));
        event.setModule(annotation.module());
        event.setOperation(annotation.operation());
        event.setUserId(UserContext.getUserId());
        event.setUsername(UserContext.getUsername());
        event.setClientIp(request != null ? IpUtil.getClientIp(request) : "unknown");
        event.setRequestUri(request != null ? request.getRequestURI() : "");
        event.setRequestMethod(request != null ? request.getMethod() : "");
        event.setStatus(status);
        event.setErrorMsg(errorMsg);
        event.setCostMs(costMs);
        event.setOperateTime(LocalDateTime.now());

        kafkaTemplate.send("edu.audit.log", event.getLogId(), event);
    }
}
