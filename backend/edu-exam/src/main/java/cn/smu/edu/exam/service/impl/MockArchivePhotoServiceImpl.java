package cn.smu.edu.exam.service.impl;

import cn.smu.edu.exam.service.ArchivePhotoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * 档案照 Mock 实现（开发/测试环境）。
 * 生产环境应提供真实实现（从 MinIO 取出 AES-256 解密档案照），
 * 替换此 Bean 即可（@Primary 或独立 Profile）。
 */
@Slf4j
@Service
@ConditionalOnMissingBean(name = "realArchivePhotoService")
public class MockArchivePhotoServiceImpl implements ArchivePhotoService {

    @Override
    public String getDecryptedArchivePhoto(Long studentId) {
        log.warn("使用 Mock 档案照服务（仅开发环境）: studentId={}", studentId);
        return "MOCK_ARCHIVE_PHOTO_" + studentId;
    }
}
