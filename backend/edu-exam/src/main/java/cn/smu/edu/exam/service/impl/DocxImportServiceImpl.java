package cn.smu.edu.exam.service.impl;

import cn.smu.edu.exam.domain.entity.Question;
import cn.smu.edu.exam.domain.entity.QuestionOption;
import cn.smu.edu.exam.domain.vo.DocxImportResultVO;
import cn.smu.edu.exam.domain.vo.QuestionImportItemVO;
import cn.smu.edu.exam.repository.QuestionMapper;
import cn.smu.edu.exam.repository.QuestionOptionMapper;
import cn.smu.edu.exam.service.DocxImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * docx 题目导入。
 *
 * <p>文档中每道题以类型标记开头（[单选题]/[多选题]/[判断题]/[填空题]/[主观题]/[投票题]），
 * 空行或下一个类型标记作为分隔。题块内必须有"答案："行（主观题允许为参考答案）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocxImportServiceImpl implements DocxImportService {

    private static final Pattern TYPE_PATTERN = Pattern.compile("^\\[(.+?)]\\s*$");
    private static final Pattern OPTION_PATTERN = Pattern.compile("^([A-E])\\s*[.、]\\s*(.+)$");
    private static final Pattern ANSWER_PATTERN = Pattern.compile("^答案[：:]\\s*(.*)$");
    private static final Pattern ANALYSIS_PATTERN = Pattern.compile("^解析[：:]\\s*(.*)$");

    private static final Map<String, Integer> TYPE_MAP = Map.ofEntries(
            Map.entry("单选题", 1), Map.entry("单选", 1),
            Map.entry("多选题", 2), Map.entry("多选", 2),
            Map.entry("判断题", 3), Map.entry("判断", 3),
            Map.entry("填空题", 4), Map.entry("填空", 4),
            Map.entry("主观题", 5), Map.entry("简答题", 5), Map.entry("论述题", 5),
            Map.entry("投票题", 6), Map.entry("投票", 6)
    );

    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;

    @Override
    @Transactional
    public DocxImportResultVO importFromDocx(Long bankId, MultipartFile file,
                                             Long teacherId, BigDecimal defaultScore) {
        List<String> lines = readDocxLines(file);
        List<QuestionBlock> blocks = parseBlocks(lines);

        int success = 0, fail = 0;
        List<QuestionImportItemVO> details = new ArrayList<>();

        for (int i = 0; i < blocks.size(); i++) {
            QuestionBlock block = blocks.get(i);
            try {
                validateBlock(block);
                insertQuestion(block, bankId, teacherId, defaultScore);
                success++;
                details.add(QuestionImportItemVO.success(i + 1, block.content));
            } catch (IllegalArgumentException e) {
                fail++;
                details.add(QuestionImportItemVO.fail(i + 1, block.content, e.getMessage()));
                log.warn("第{}题导入失败: {}", i + 1, e.getMessage());
            }
        }

        log.info("docx导题完成: bankId={}, total={}, success={}, fail={}", bankId, blocks.size(), success, fail);
        return new DocxImportResultVO(blocks.size(), success, fail, details);
    }

    // ── 读取 docx 文本行 ─────────────────────────────────────────────────────

    public List<String> readDocxLines(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             XWPFDocument doc = new XWPFDocument(is)) {
            return doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalArgumentException("文件读取失败：" + e.getMessage());
        }
    }

    // ── 解析题块（可独立测试） ─────────────────────────────────────────────────

    /**
     * 将文本行列表解析为题目块列表。
     * 题块以 [xxx] 标记行开始，遇到下一个标记或文档末尾结束。
     */
    public static List<QuestionBlock> parseBlocks(List<String> lines) {
        List<QuestionBlock> blocks = new ArrayList<>();
        QuestionBlock current = null;
        List<String> contentBuffer = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.trim();
            Matcher typeMatcher = TYPE_PATTERN.matcher(line);

            if (typeMatcher.matches()) {
                // 新题型标记 → 保存前一个 block
                if (current != null) {
                    flushBlock(current, contentBuffer, blocks);
                }
                String typeName = typeMatcher.group(1).trim();
                Integer typeCode = TYPE_MAP.get(typeName);
                current = new QuestionBlock();
                current.typeName = typeName;
                current.questionType = typeCode != null ? typeCode : -1;
                contentBuffer = new ArrayList<>();
                continue;
            }

            if (current == null) continue; // 标记前的内容忽略

            Matcher answerMatcher = ANSWER_PATTERN.matcher(line);
            if (answerMatcher.matches()) {
                current.answer = answerMatcher.group(1).trim();
                continue;
            }

            Matcher analysisMatcher = ANALYSIS_PATTERN.matcher(line);
            if (analysisMatcher.matches()) {
                current.analysis = analysisMatcher.group(1).trim();
                continue;
            }

            Matcher optionMatcher = OPTION_PATTERN.matcher(line);
            if (optionMatcher.matches()) {
                current.options.add(new OptionEntry(
                        optionMatcher.group(1).toUpperCase(),
                        optionMatcher.group(2).trim()));
                continue;
            }

            // 普通文本 → 加入内容 buffer
            if (!line.isEmpty()) {
                contentBuffer.add(line);
            }
        }

        if (current != null) {
            flushBlock(current, contentBuffer, blocks);
        }

        return blocks;
    }

    private static void flushBlock(QuestionBlock block, List<String> contentBuffer,
                                    List<QuestionBlock> blocks) {
        block.content = String.join("\n", contentBuffer).trim();
        blocks.add(block);
    }

    // ── 校验 ─────────────────────────────────────────────────────────────────

    private static void validateBlock(QuestionBlock block) {
        if (block.questionType == -1) {
            throw new IllegalArgumentException("未知题型：" + block.typeName);
        }
        if (block.content == null || block.content.isBlank()) {
            throw new IllegalArgumentException("题干不能为空");
        }
        // 选择题/投票题必须有选项
        if (block.questionType == 1 || block.questionType == 2 || block.questionType == 6) {
            if (block.options.isEmpty()) {
                throw new IllegalArgumentException("选择题/投票题必须包含选项（A-E 开头）");
            }
        }
        // 单选/多选/判断/填空必须有答案
        if (block.questionType <= 4 && (block.answer == null || block.answer.isBlank())) {
            throw new IllegalArgumentException("客观题必须包含「答案：」行");
        }
    }

    // ── 持久化 ──────────────────────────────────────────────────────────────

    private void insertQuestion(QuestionBlock block, Long bankId, Long teacherId,
                                 BigDecimal defaultScore) {
        Question q = new Question();
        q.setBankId(bankId);
        q.setType(block.questionType);
        q.setContent(block.content);
        q.setAnswer(block.answer);
        q.setAnalysis(block.analysis);
        q.setScore(defaultScore);
        q.setCreatorId(teacherId);
        questionMapper.insert(q);

        if (!block.options.isEmpty()) {
            for (int i = 0; i < block.options.size(); i++) {
                OptionEntry opt = block.options.get(i);
                QuestionOption qo = new QuestionOption();
                qo.setQuestionId(q.getId());
                qo.setOptionLabel(opt.label);
                qo.setContent(opt.content);
                qo.setSortOrder(i + 1);
                // 多选题答案可能是 "A,C"，用逗号分隔检测；单选答案是单字母
                qo.setIsCorrect(isOptionCorrect(block.questionType, block.answer, opt.label) ? 1 : 0);
                questionOptionMapper.insert(qo);
            }
        }
    }

    private static boolean isOptionCorrect(int questionType, String answer, String label) {
        if (answer == null) return false;
        if (questionType == 2) { // 多选：答案可能是 "A,B,C"
            String normalized = answer.toUpperCase().replace("，", ",");
            return Arrays.asList(normalized.split(",")).contains(label);
        }
        return label.equalsIgnoreCase(answer.trim());
    }

    // ── 内部数据结构 ─────────────────────────────────────────────────────────

    public static class QuestionBlock {
        public String typeName;
        public int questionType;
        public String content;
        public String answer;
        public String analysis;
        public List<OptionEntry> options = new ArrayList<>();
    }

    public static class OptionEntry {
        public final String label;
        public final String content;

        public OptionEntry(String label, String content) {
            this.label = label;
            this.content = content;
        }
    }
}
