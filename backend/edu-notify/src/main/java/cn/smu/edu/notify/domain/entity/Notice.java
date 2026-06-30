package cn.smu.edu.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知公告（{@code notice}），支持全校/院系/班级三级范围推送。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long senderId;
    private String senderName;
    private String title;
    private String content;

    /** 发送范围：SCHOOL-全校 DEPT-指定院系 CLASS-指定班级。 */
    private String scope;
    /** 指定院系ID（scope=DEPT 时有效）。 */
    private Long deptId;
    /** 指定班级ID（scope=CLASS 时有效）。 */
    private Long classId;
    /** 目标角色过滤：ALL/TEACHER/STUDENT。 */
    private String targetRoles;

    /** 是否需要审核：0-直接发布 1-需审核。 */
    private Integer needReview;
    /** 状态：0-草稿 1-审核中 2-已发布 3-已撤回。 */
    private Integer status;

    private Integer sendCount;
    private Integer readCount;

    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
