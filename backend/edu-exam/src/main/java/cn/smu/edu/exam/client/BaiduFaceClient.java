package cn.smu.edu.exam.client;

/**
 * 百度人脸核验 API 客户端接口。
 * 依赖倒置：FaceVerifyServiceImpl 依赖此接口，便于单元测试 Mock。
 */
public interface BaiduFaceClient {

    record FaceMatchResult(boolean matched, double score) {}

    /**
     * 比对两张人脸图片的相似度。
     *
     * @param livePhotoBase64    现场实时拍照（base64）
     * @param archivePhotoBase64 档案标准照（base64），由 ArchivePhotoService 提供
     * @return 比对结果（matched=相似度是否达标，score=相似度分值0~100）
     */
    FaceMatchResult match(String livePhotoBase64, String archivePhotoBase64);
}
