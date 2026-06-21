package cn.smu.edu.exam.controller;

import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.exam.domain.dto.FaceVerifyDTO;
import cn.smu.edu.exam.domain.vo.FaceVerifyResultVO;
import cn.smu.edu.exam.service.FaceVerifyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exam/publishes")
@RequiredArgsConstructor
public class FaceVerifyController {

    private final FaceVerifyService faceVerifyService;

    /**
     * 人脸核验（学生端）。
     * C6合规：livePhotoBase64 只在服务端内存处理，不写库，响应不含原始照片。
     * 通过后 session_status 从 VERIFYING 变为 ANSWERING，方可作答。
     */
    @PostMapping("/{publishId}/face-verify")
    public Result<FaceVerifyResultVO> verify(
            @PathVariable Long publishId,
            @Valid @RequestBody FaceVerifyDTO dto) {
        return Result.ok(faceVerifyService.verify(publishId, UserContext.getUserId(), dto));
    }
}
