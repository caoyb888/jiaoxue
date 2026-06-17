package cn.smu.edu.interaction.service;

import cn.smu.edu.interaction.domain.vo.AttendCodeVO;

public interface AttendanceCodeService {

    /**
     * 生成签到码（口令 + 二维码 Token），存 Redis TTL=5min，写 attendance_code 表
     *
     * @param lessonId  课堂 ID
     * @param teacherId 发起教师 ID
     */
    AttendCodeVO generateCode(Long lessonId, Long teacherId);

    /**
     * 查询当前有效签到码（教师刷新页面后可回显）
     */
    AttendCodeVO getCurrentCode(Long lessonId);
}
