package cn.smu.edu.notify.service;

import cn.smu.edu.notify.domain.entity.Notice;
import cn.smu.edu.notify.repository.NoticeTargetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 按通知范围解析目标用户 ID（发布时统计人数、推送时确定收件人，单一来源）。
 */
@Component
@RequiredArgsConstructor
public class NoticeTargetResolver {

    private final NoticeTargetMapper targetMapper;

    /** 解析通知的目标用户 ID 列表。 */
    public List<Long> resolve(Notice notice) {
        return switch (notice.getScope() == null ? "" : notice.getScope()) {
            case "CLASS" -> targetMapper.selectClassStudentIds(notice.getClassId());
            case "DEPT" -> targetMapper.selectUserIds(notice.getDeptId(), userType(notice.getTargetRoles()));
            case "SCHOOL" -> targetMapper.selectUserIds(null, userType(notice.getTargetRoles()));
            default -> List.of();
        };
    }

    /** 目标角色 → sys_user.user_type；ALL/未知不过滤。 */
    private Integer userType(String targetRoles) {
        if ("TEACHER".equals(targetRoles)) {
            return 2;
        }
        if ("STUDENT".equals(targetRoles)) {
            return 1;
        }
        return null;
    }
}
