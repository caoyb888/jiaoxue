package cn.smu.edu.notify.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 学生/教师接收端的通知列表项（含当前用户已读标记）。 */
@Data
public class NoticeItemVO {
    private Long id;
    private String title;
    private String content;
    private String senderName;
    private String scope;
    private LocalDateTime publishedAt;
    /** 当前用户是否已读。 */
    private Boolean read;
}
