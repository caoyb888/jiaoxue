package cn.smu.edu.exam.service;

import cn.smu.edu.exam.domain.dto.FaceVerifyDTO;
import cn.smu.edu.exam.domain.vo.FaceVerifyResultVO;

public interface FaceVerifyService {

    /**
     * 执行人脸核验。
     * C6合规：live photo 只在内存中使用，比对完成后丢弃，永远不写库。
     * 通过后将 exam_monitor.session_status 从 VERIFYING 更新为 ANSWERING。
     */
    FaceVerifyResultVO verify(Long publishId, Long studentId, FaceVerifyDTO dto);
}
