package cn.smu.edu.exam.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HeartbeatVO {

    private String sessionStatus;
    private LocalDateTime lastHeartbeatAt;
}
