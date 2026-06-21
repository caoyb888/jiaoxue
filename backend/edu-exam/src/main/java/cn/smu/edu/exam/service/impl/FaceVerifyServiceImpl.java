package cn.smu.edu.exam.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.exam.client.BaiduFaceClient;
import cn.smu.edu.exam.config.BaiduFaceConfig;
import cn.smu.edu.exam.domain.dto.FaceVerifyDTO;
import cn.smu.edu.exam.domain.entity.ExamMonitor;
import cn.smu.edu.exam.domain.entity.ExamPublish;
import cn.smu.edu.exam.domain.vo.FaceVerifyResultVO;
import cn.smu.edu.exam.repository.ExamMonitorMapper;
import cn.smu.edu.exam.repository.ExamPublishMapper;
import cn.smu.edu.exam.service.ArchivePhotoService;
import cn.smu.edu.exam.service.FaceVerifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * C6合规要点：
 *   1. livePhotoBase64 只在内存传递给 BaiduFaceClient，调用后即离开作用域被 GC
 *   2. archivePhotoBase64 同上，不写入任何持久存储
 *   3. exam_monitor 只记录 face_verify_passed（0/1）和 face_verify_score（BigDecimal）
 *   4. 响应 VO 无 rawPhoto 字段
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FaceVerifyServiceImpl implements FaceVerifyService {

    private final ExamPublishMapper publishMapper;
    private final ExamMonitorMapper monitorMapper;
    private final BaiduFaceClient baiduFaceClient;
    private final ArchivePhotoService archivePhotoService;
    private final BaiduFaceConfig baiduFaceConfig;

    @Override
    @Transactional
    public FaceVerifyResultVO verify(Long publishId, Long studentId, FaceVerifyDTO dto) {
        ExamPublish publish = publishMapper.selectById(publishId);
        if (publish == null) {
            throw new BizException(ErrorCode.EXAM_NOT_FOUND);
        }

        // 本场考试必须配置了人脸核验才允许调用
        if (publish.getFaceVerifyType() == null || publish.getFaceVerifyType() == 0) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "本场考试未开启人脸核验");
        }

        ExamMonitor monitor = monitorMapper.selectByPublishAndStudent(publishId, studentId);
        if (monitor == null) {
            throw new BizException(ErrorCode.EXAM_SESSION_NOT_FOUND);
        }
        if ("SUBMITTED".equals(monitor.getSessionStatus())) {
            throw new BizException(ErrorCode.EXAM_ALREADY_SUBMITTED);
        }

        // 获取学生档案照（生产环境：AES-256 解密后 base64，开发环境：Mock）
        // C6：档案照只用于比对，不返回给调用方，不写入 DB
        String archivePhoto = archivePhotoService.getDecryptedArchivePhoto(studentId);
        if (archivePhoto == null) {
            log.warn("学生无档案照，跳过人脸核验: studentId={}", studentId);
            throw new BizException(ErrorCode.FACE_VERIFY_FAIL.getCode(), "学生无档案照，无法完成核验");
        }

        // 调用百度人脸比对（live photo 与 archive photo 均在内存，不持久化）
        BaiduFaceClient.FaceMatchResult matchResult = baiduFaceClient.match(
                dto.getLivePhotoBase64(), archivePhoto);

        // C6：更新监考记录，只存比对结果，NEVER 存原始照片
        monitor.setFaceVerifyPassed(matchResult.matched() ? 1 : 0);
        monitor.setFaceVerifyScore(BigDecimal.valueOf(matchResult.score()));

        if (matchResult.matched()) {
            monitor.setSessionStatus("ANSWERING");
            log.info("人脸核验通过: publishId={}, studentId={}, score={}", publishId, studentId, matchResult.score());
        } else {
            log.warn("人脸核验未通过: publishId={}, studentId={}, score={}, threshold={}",
                    publishId, studentId, matchResult.score(), baiduFaceConfig.getPassThreshold());
        }
        monitorMapper.updateById(monitor);

        FaceVerifyResultVO vo = new FaceVerifyResultVO();
        vo.setPassed(matchResult.matched());
        vo.setScore(BigDecimal.valueOf(matchResult.score()));
        vo.setSessionStatus(monitor.getSessionStatus());
        vo.setMessage(matchResult.matched() ? "核验通过，可以开始作答" : "核验未通过，请重新拍照");
        return vo;
    }
}
