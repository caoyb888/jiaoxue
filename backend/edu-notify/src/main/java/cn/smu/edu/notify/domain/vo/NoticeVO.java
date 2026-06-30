package cn.smu.edu.notify.domain.vo;

import cn.smu.edu.notify.domain.entity.Notice;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 通知公告响应。 */
@Data
@Builder
public class NoticeVO {

    private Long id;
    private Long senderId;
    private String senderName;
    private String title;
    private String content;
    private String scope;
    private Long deptId;
    private Long classId;
    private String targetRoles;
    private Integer status;
    private Integer sendCount;
    private Integer readCount;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    public static NoticeVO from(Notice n) {
        return NoticeVO.builder()
                .id(n.getId())
                .senderId(n.getSenderId())
                .senderName(n.getSenderName())
                .title(n.getTitle())
                .content(n.getContent())
                .scope(n.getScope())
                .deptId(n.getDeptId())
                .classId(n.getClassId())
                .targetRoles(n.getTargetRoles())
                .status(n.getStatus())
                .sendCount(n.getSendCount())
                .readCount(n.getReadCount())
                .publishedAt(n.getPublishedAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
