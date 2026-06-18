package cn.smu.edu.exam.service;

/**
 * 学生档案照服务接口。
 * 生产环境：从 MinIO 取出 AES-256 加密档案照并解密为 base64。
 * 开发环境：MockArchivePhotoService 返回占位数据。
 * C6合规：档案照只用于人脸比对，不返回给前端，不写入业务表。
 */
public interface ArchivePhotoService {

    /**
     * 获取学生档案标准照（base64，已解密）。
     *
     * @return base64字符串，null 表示该学生无档案照
     */
    String getDecryptedArchivePhoto(Long studentId);
}
