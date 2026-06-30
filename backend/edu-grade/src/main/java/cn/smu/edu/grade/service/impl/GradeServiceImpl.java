package cn.smu.edu.grade.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.grade.domain.entity.StudentGrade;
import cn.smu.edu.grade.domain.vo.OfflineImportResultVO;
import cn.smu.edu.grade.domain.vo.StudentGradeVO;
import cn.smu.edu.grade.repository.StudentGradeMapper;
import cn.smu.edu.grade.service.GradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link GradeService} 实现：成绩列表查询 + 线下成绩 xlsx 导入。
 *
 * <p>线下成绩为独立项，不参与综合总分自动加权（见 {@link GradeCalcServiceImpl} S8-08），
 * 因此导入仅写 {@code offline_score}，不触发重算。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {

    /** 满分上限，导入成绩超出则判为非法行。 */
    private static final BigDecimal MAX_SCORE = new BigDecimal("100");

    private final StudentGradeMapper studentGradeMapper;

    @Override
    public List<StudentGradeVO> listClassGrades(Long classId) {
        return studentGradeMapper.selectByClassId(classId).stream()
                .map(StudentGradeVO::from)
                .toList();
    }

    @Override
    public OfflineImportResultVO importOffline(Long classId, MultipartFile file) {
        Map<Long, StudentGrade> existing = studentGradeMapper.selectByClassId(classId).stream()
                .collect(Collectors.toMap(StudentGrade::getStudentId, Function.identity(), (a, b) -> a));

        List<String> errors = new ArrayList<>();
        int total = 0;
        int success = 0;
        DataFormatter formatter = new DataFormatter();

        try (InputStream in = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            // 第 0 行为表头（学号 | 线下成绩），从第 1 行开始解析
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (isBlankRow(row, formatter)) {
                    continue;
                }
                total++;
                int excelRow = r + 1; // 给用户看的 1-based 行号
                try {
                    Long studentId = parseStudentId(formatter.formatCellValue(row.getCell(0)));
                    BigDecimal score = parseScore(formatter.formatCellValue(row.getCell(1)));
                    upsertOffline(classId, studentId, score, existing);
                    success++;
                } catch (IllegalArgumentException e) {
                    errors.add("第 " + excelRow + " 行：" + e.getMessage());
                }
            }
        } catch (IOException | RuntimeException e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR.getCode(), "线下成绩解析失败: " + e.getMessage());
        }

        log.info("线下成绩导入: classId={}, 总行数={}, 成功={}, 失败={}",
                classId, total, success, errors.size());
        return new OfflineImportResultVO(total, success, errors.size(), errors);
    }

    /** 已存在则更新 offline_score，否则插入新行（其余维度默认 0、待计算）。 */
    private void upsertOffline(Long classId, Long studentId, BigDecimal score,
                               Map<Long, StudentGrade> existing) {
        StudentGrade current = existing.get(studentId);
        if (current != null) {
            StudentGrade update = new StudentGrade();
            update.setId(current.getId());
            update.setOfflineScore(score);
            studentGradeMapper.updateById(update);
        } else {
            StudentGrade insert = StudentGrade.builder()
                    .classId(classId)
                    .studentId(studentId)
                    .offlineScore(score)
                    .calcStatus(0)
                    .build();
            studentGradeMapper.insert(insert);
            existing.put(studentId, insert); // 防同文件内重复学号再次插入触发唯一键冲突
        }
    }

    private Long parseStudentId(String raw) {
        String v = raw == null ? "" : raw.trim();
        if (v.isEmpty()) {
            throw new IllegalArgumentException("学号为空");
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("学号非法: " + v);
        }
    }

    private BigDecimal parseScore(String raw) {
        String v = raw == null ? "" : raw.trim();
        if (v.isEmpty()) {
            throw new IllegalArgumentException("线下成绩为空");
        }
        BigDecimal score;
        try {
            score = new BigDecimal(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("线下成绩非法: " + v);
        }
        if (score.signum() < 0 || score.compareTo(MAX_SCORE) > 0) {
            throw new IllegalArgumentException("线下成绩超出范围[0,100]: " + v);
        }
        return score;
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        Cell a = row.getCell(0);
        Cell b = row.getCell(1);
        return formatter.formatCellValue(a).trim().isEmpty()
                && formatter.formatCellValue(b).trim().isEmpty();
    }
}
