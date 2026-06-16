package cn.smu.edu.common.aop;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogEvent {
    private String logId;
    private String module;
    private String operation;
    private Long userId;
    private String username;
    private String clientIp;
    private String requestUri;
    private String requestMethod;
    private String status;
    private String errorMsg;
    private Long costMs;
    private LocalDateTime operateTime;
}
