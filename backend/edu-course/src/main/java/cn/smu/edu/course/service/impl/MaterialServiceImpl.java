package cn.smu.edu.course.service.impl;

import cn.smu.edu.common.constant.KafkaTopic;
import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.course.domain.dto.MaterialCompleteDTO;
import cn.smu.edu.course.domain.dto.MaterialUploadDTO;
import cn.smu.edu.course.domain.entity.CourseMaterial;
import cn.smu.edu.course.domain.vo.MaterialCompleteVO;
import cn.smu.edu.course.domain.vo.MaterialListItemVO;
import cn.smu.edu.course.domain.vo.MaterialUploadVO;
import cn.smu.edu.course.event.MaterialConvertEvent;
import cn.smu.edu.course.repository.CourseMaterialMapper;
import cn.smu.edu.course.service.MaterialService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private static final String MATERIAL_BUCKET = "edu-slides";
    private static final int PRESIGN_EXPIRE_MIN = 60;

    private final CourseMaterialMapper materialMapper;
    private final MinioClient minioClient;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${minio.endpoint:http://100.84.68.115:19000}")
    private String minioEndpoint;

    @Override
    public MaterialUploadVO applyUpload(Long teacherId, MaterialUploadDTO dto) {
        String uploadId = UUID.randomUUID().toString();
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String objectPath = String.format("materials/teacher-%d/%s/%s.%s",
                teacherId, dateDir, uploadId, dto.getFileType());

        String presignedUrl = generatePresignedPutUrl(MATERIAL_BUCKET, objectPath);

        // Redis 暂存上传凭证：TTL 1h（等于预签名URL有效期）
        String ticketKey = "material:upload:" + uploadId;
        redisTemplate.opsForHash().putAll(ticketKey, Map.of(
                "teacherId", teacherId.toString(),
                "objectPath", objectPath,
                "fileType", dto.getFileType(),
                "fileSizeKb", dto.getFileSizeKb().toString(),
                "fileName", dto.getFileName()
        ));
        redisTemplate.expire(ticketKey, PRESIGN_EXPIRE_MIN, TimeUnit.MINUTES);

        log.info("课件上传凭证生成: uploadId={}, teacherId={}, fileType={}", uploadId, teacherId, dto.getFileType());

        return MaterialUploadVO.builder()
                .uploadId(uploadId)
                .presignedUrl(presignedUrl)
                .expiresIn(PRESIGN_EXPIRE_MIN * 60)
                .objectPath(objectPath)
                .build();
    }

    @Override
    public MaterialCompleteVO completeUpload(Long teacherId, MaterialCompleteDTO dto) {
        String ticketKey = "material:upload:" + dto.getUploadId();
        Map<Object, Object> ticket = redisTemplate.opsForHash().entries(ticketKey);

        if (ticket.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "上传凭证不存在或已过期，请重新申请上传");
        }
        if (!teacherId.toString().equals(ticket.get("teacherId"))) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "无权操作此上传凭证");
        }

        String objectPath = (String) ticket.get("objectPath");
        String fileType = (String) ticket.get("fileType");
        int fileSizeKb = Integer.parseInt((String) ticket.get("fileSizeKb"));

        // mp4 需要人工审核（status=0），其他格式直接触发转换
        boolean needConvert = !fileType.equals("mp4");
        int initialStatus = needConvert ? 0 : 0;

        CourseMaterial material = CourseMaterial.builder()
                .teacherId(teacherId)
                .title(dto.getTitle())
                .fileType(fileType)
                .originalPath(objectPath)
                .pageCount(0)
                .fileSizeKb(fileSizeKb)
                .status(initialStatus)
                .build();
        materialMapper.insert(material);

        redisTemplate.delete(ticketKey);

        String message;
        if (needConvert) {
            String slideDir = String.format("slides/%d/", material.getId());
            MaterialConvertEvent event = MaterialConvertEvent.builder()
                    .materialId(material.getId())
                    .teacherId(teacherId)
                    .originalPath(objectPath)
                    .fileType(fileType)
                    .bucketName(MATERIAL_BUCKET)
                    .slideDir(slideDir)
                    .build();
            try {
                kafkaTemplate.send(KafkaTopic.MATERIAL_CONVERT, material.getId().toString(), event);
                log.info("课件转换任务已发送: materialId={}, fileType={}", material.getId(), fileType);
            } catch (Exception e) {
                log.warn("课件转换Kafka消息发送失败，不影响主流程: materialId={}", material.getId(), e);
            }
            message = "课件上传成功，正在转换为图片序列，请等待约2~5分钟";
        } else {
            message = "视频上传成功，正在审核中，审核通过后可使用";
        }

        log.info("课件上传完成: materialId={}, teacherId={}, fileType={}", material.getId(), teacherId, fileType);

        return MaterialCompleteVO.builder()
                .materialId(material.getId())
                .status(initialStatus)
                .message(message)
                .build();
    }

    @Override
    public PageResult<MaterialListItemVO> listMaterials(Long teacherId, String keyword, int page, int size) {
        Page<CourseMaterial> pageParam = new Page<>(page, size);
        var result = materialMapper.selectMaterialPage(pageParam, teacherId,
                keyword != null && !keyword.isBlank() ? keyword : null);

        List<MaterialListItemVO> voList = result.getRecords().stream()
                .map(m -> MaterialListItemVO.builder()
                        .id(m.getId())
                        .title(m.getTitle())
                        .fileType(m.getFileType())
                        .pageCount(m.getPageCount())
                        .status(m.getStatus())
                        .fileSizeKb(m.getFileSizeKb())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), page, size);
    }

    private String generatePresignedPutUrl(String bucket, String objectPath) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucket)
                    .object(objectPath)
                    .expiry(PRESIGN_EXPIRE_MIN, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            log.error("生成预签名上传URL失败: bucket={}, object={}", bucket, objectPath, e);
            throw new BizException(ErrorCode.SYSTEM_ERROR.getCode(), "生成上传链接失败，请稍后重试");
        }
    }
}
