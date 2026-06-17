package cn.smu.edu.interaction.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AttendCodeVO {

    private Long lessonId;

    /** 口令（学生手动输入） */
    private String code;

    /** 二维码 token（前端据此生成二维码） */
    private String qrToken;

    /** 过期时间 */
    private LocalDateTime expireAt;

    /** Redis 中剩余有效秒数 */
    private long remainSeconds;
}
