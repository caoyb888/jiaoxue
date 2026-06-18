package cn.smu.edu.file.util;

import cn.smu.edu.file.config.MinioProperties;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 工具类 — 封装预签名 URL 生成、对象操作
 * 上传三步流程：
 *   1. POST /api/v1/file/presign → 后端生成 presignedPutUrl
 *   2. 前端直接 PUT MinIO（绕过后端，节省带宽）
 *   3. POST /api/v1/file/complete → 后端接收完成通知，写入 file_object 表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioClient minioClient;
    private final MinioProperties props;

    public String generatePresignedPutUrl(String bucketName, String objectPath) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucketName)
                    .object(objectPath)
                    .expiry(props.getPresignExpireMinutes(), TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            log.error("生成预签名上传URL失败: bucket={}, object={}", bucketName, objectPath, e);
            throw new RuntimeException("生成上传链接失败", e);
        }
    }

    public String generatePresignedGetUrl(String bucketName, String objectPath) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectPath)
                    .expiry(props.getPresignExpireMinutes(), TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            log.error("生成预签名下载URL失败: bucket={}, object={}", bucketName, objectPath, e);
            throw new RuntimeException("生成下载链接失败", e);
        }
    }

    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            log.warn("检查 bucket 是否存在失败: bucket={}", bucketName, e);
            return false;
        }
    }

    public void createBucket(String bucketName) {
        try {
            if (!bucketExists(bucketName)) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO Bucket 创建成功: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("创建 MinIO Bucket 失败: bucket={}", bucketName, e);
            throw new RuntimeException("创建 Bucket 失败: " + bucketName, e);
        }
    }

    public void deleteObject(String bucketName, String objectPath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectPath)
                    .build());
            log.info("文件删除成功: bucket={}, object={}", bucketName, objectPath);
        } catch (Exception e) {
            log.error("删除 MinIO 对象失败: bucket={}, object={}", bucketName, objectPath, e);
            throw new RuntimeException("删除文件失败", e);
        }
    }

    public boolean objectExists(String bucketName, String objectPath) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectPath)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
