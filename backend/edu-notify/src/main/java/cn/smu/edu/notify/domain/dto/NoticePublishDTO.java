package cn.smu.edu.notify.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 发布通知公告请求。 */
@Data
public class NoticePublishDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不超过200字")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    /** 发送范围：SCHOOL-全校 DEPT-指定院系 CLASS-指定班级。 */
    @Pattern(regexp = "SCHOOL|DEPT|CLASS", message = "范围非法（SCHOOL/DEPT/CLASS）")
    private String scope;

    /** 指定院系ID（scope=DEPT 必填）。 */
    private Long deptId;

    /** 指定班级ID（scope=CLASS 必填）。 */
    private Long classId;

    /** 目标角色过滤：ALL/TEACHER/STUDENT，默认 ALL。 */
    @Pattern(regexp = "ALL|TEACHER|STUDENT", message = "目标角色非法（ALL/TEACHER/STUDENT）")
    private String targetRoles = "ALL";

    /** 发送人显示名称（可自定义，留空用当前用户名）。 */
    private String senderName;
}
