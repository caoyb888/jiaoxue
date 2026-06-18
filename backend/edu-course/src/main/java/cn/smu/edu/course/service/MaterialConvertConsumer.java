package cn.smu.edu.course.service;

import cn.smu.edu.course.event.MaterialConvertEvent;
import cn.smu.edu.course.repository.CourseMaterialMapper;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 课件转换消费者：
 *   edu.material.convert → 下载原始文件 → LibreOffice 转 PNG → 上传序列到 MinIO
 * concurrency = "2"：LibreOffice 进程占内存，限制并发度
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialConvertConsumer {

    private final MinioClient minioClient;
    private final CourseMaterialMapper materialMapper;

    @KafkaListener(topics = "edu.material.convert", groupId = "edu-course-convert",
            concurrency = "2")
    public void handleConvert(MaterialConvertEvent event) {
        Long materialId = event.getMaterialId();
        log.info("开始转换课件: materialId={}, fileType={}", materialId, event.getFileType());

        Path tmpDir = null;
        try {
            tmpDir = Files.createTempDirectory("edu-convert-" + materialId + "-");
            Path inputFile = tmpDir.resolve("input." + event.getFileType());
            Path outputDir = tmpDir.resolve("output");
            Files.createDirectory(outputDir);

            downloadFromMinio(event.getBucketName(), event.getOriginalPath(), inputFile);

            int pageCount = runLibreOfficeConvert(inputFile, outputDir, event.getFileType());

            List<File> pngFiles = sortedPngFiles(outputDir);
            if (pngFiles.isEmpty()) {
                log.warn("LibreOffice 转换结果为空: materialId={}", materialId);
                materialMapper.updateConvertResult(materialId, null, 0, 2);
                return;
            }

            uploadImageSequence(pngFiles, event.getBucketName(), event.getSlideDir());

            materialMapper.updateConvertResult(materialId, event.getSlideDir(), pngFiles.size(), 1);
            log.info("课件转换完成: materialId={}, pageCount={}", materialId, pngFiles.size());

        } catch (Exception e) {
            log.error("课件转换失败: materialId={}", materialId, e);
            materialMapper.updateConvertResult(materialId, null, 0, 2);
        } finally {
            deleteTempDir(tmpDir);
        }
    }

    private void downloadFromMinio(String bucket, String objectPath, Path dest) throws Exception {
        try (InputStream is = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectPath)
                .build())) {
            Files.copy(is, dest);
        }
    }

    private int runLibreOfficeConvert(Path inputFile, Path outputDir, String fileType) throws Exception {
        String convertTarget = "png:impress_png_Export";
        if (fileType.equals("pdf")) {
            convertTarget = "png:draw_png_Export";
        } else if (fileType.equals("docx")) {
            convertTarget = "png:writer_png_Export";
        }

        ProcessBuilder pb = new ProcessBuilder(
                "soffice", "--headless",
                "--convert-to", convertTarget,
                "--outdir", outputDir.toAbsolutePath().toString(),
                inputFile.toAbsolutePath().toString()
        );
        pb.redirectErrorStream(true);
        pb.directory(outputDir.toFile());

        Process process = pb.start();
        boolean finished = process.waitFor(5, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("LibreOffice 转换超时（>5min）");
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("LibreOffice 进程退出码: " + process.exitValue());
        }
        return 0;
    }

    private List<File> sortedPngFiles(Path outputDir) {
        File[] files = outputDir.toFile().listFiles(f -> f.getName().endsWith(".png"));
        if (files == null || files.length == 0) return List.of();
        return Arrays.stream(files)
                .sorted(Comparator.comparing(File::getName))
                .toList();
    }

    private void uploadImageSequence(List<File> pngFiles, String bucket, String slideDir) throws Exception {
        for (int i = 0; i < pngFiles.size(); i++) {
            File png = pngFiles.get(i);
            String objectPath = String.format("%sslide_%04d.png", slideDir, i + 1);
            try (InputStream is = new FileInputStream(png)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectPath)
                        .stream(is, png.length(), -1)
                        .contentType("image/png")
                        .build());
            }
        }
    }

    private void deleteTempDir(Path tmpDir) {
        if (tmpDir == null) return;
        try {
            Files.walk(tmpDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception e) {
            log.warn("清理临时目录失败: {}", tmpDir, e);
        }
    }
}
