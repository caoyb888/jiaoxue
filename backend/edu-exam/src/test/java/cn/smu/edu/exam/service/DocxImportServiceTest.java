package cn.smu.edu.exam.service;

import cn.smu.edu.exam.domain.vo.DocxImportResultVO;
import cn.smu.edu.exam.repository.QuestionMapper;
import cn.smu.edu.exam.repository.QuestionOptionMapper;
import cn.smu.edu.exam.service.impl.DocxImportServiceImpl;
import cn.smu.edu.exam.service.impl.DocxImportServiceImpl.QuestionBlock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocxImportServiceTest {

    @Mock private QuestionMapper questionMapper;
    @Mock private QuestionOptionMapper questionOptionMapper;

    @InjectMocks private DocxImportServiceImpl service;

    // ── parseBlocks 解析器测试（纯静态，无 DB 依赖）────────────────────────

    @Test
    void parseBlocks_singleChoice_parsedCorrectly() {
        List<String> lines = lines(
                "[单选题]",
                "下面哪个不是Java的基本数据类型？",
                "A. int",
                "B. String",
                "C. double",
                "D. float",
                "答案：B",
                "解析：String是引用类型。"
        );

        List<QuestionBlock> blocks = DocxImportServiceImpl.parseBlocks(lines);

        assertThat(blocks).hasSize(1);
        QuestionBlock b = blocks.get(0);
        assertThat(b.questionType).isEqualTo(1);
        assertThat(b.content).isEqualTo("下面哪个不是Java的基本数据类型？");
        assertThat(b.options).hasSize(4);
        assertThat(b.options.get(1).label).isEqualTo("B");
        assertThat(b.options.get(1).content).isEqualTo("String");
        assertThat(b.answer).isEqualTo("B");
        assertThat(b.analysis).isEqualTo("String是引用类型。");
    }

    @Test
    void parseBlocks_multipleChoice_commaSeparatedAnswer() {
        List<QuestionBlock> blocks = DocxImportServiceImpl.parseBlocks(lines(
                "[多选题]",
                "以下属于网络层协议的有？",
                "A. IP",
                "B. TCP",
                "C. ICMP",
                "D. HTTP",
                "答案：A,C"
        ));

        assertThat(blocks).hasSize(1);
        QuestionBlock b = blocks.get(0);
        assertThat(b.questionType).isEqualTo(2);
        assertThat(b.answer).isEqualTo("A,C");
        assertThat(b.options).hasSize(4);
    }

    @Test
    void parseBlocks_trueFalse_noOptions() {
        List<QuestionBlock> blocks = DocxImportServiceImpl.parseBlocks(lines(
                "[判断题]",
                "Java是静态语言。",
                "答案：正确"
        ));

        assertThat(blocks).hasSize(1);
        QuestionBlock b = blocks.get(0);
        assertThat(b.questionType).isEqualTo(3);
        assertThat(b.options).isEmpty();
        assertThat(b.answer).isEqualTo("正确");
    }

    @Test
    void parseBlocks_essay_noAnswer_isAllowed() {
        List<QuestionBlock> blocks = DocxImportServiceImpl.parseBlocks(lines(
                "[主观题]",
                "请简述面向对象的三个基本特征。"
        ));

        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).questionType).isEqualTo(5);
        assertThat(blocks.get(0).answer).isNull();
    }

    @Test
    void parseBlocks_fillBlank() {
        List<QuestionBlock> blocks = DocxImportServiceImpl.parseBlocks(lines(
                "[填空题]",
                "Java的基本数据类型有___种。",
                "答案：8"
        ));
        assertThat(blocks.get(0).questionType).isEqualTo(4);
        assertThat(blocks.get(0).answer).isEqualTo("8");
    }

    @Test
    void parseBlocks_voteQuestion() {
        List<QuestionBlock> blocks = DocxImportServiceImpl.parseBlocks(lines(
                "[投票题]",
                "你最喜欢哪种编程语言？",
                "A. Java",
                "B. Python",
                "C. Go"
        ));
        assertThat(blocks.get(0).questionType).isEqualTo(6);
        assertThat(blocks.get(0).options).hasSize(3);
    }

    @Test
    void parseBlocks_multipleQuestions_separatedByBlankLine() {
        List<String> lines = lines(
                "[单选题]",
                "题目1",
                "A. 选项A",
                "B. 选项B",
                "答案：A",
                "",
                "[判断题]",
                "题目2",
                "答案：正确"
        );

        List<QuestionBlock> blocks = DocxImportServiceImpl.parseBlocks(lines);
        assertThat(blocks).hasSize(2);
        assertThat(blocks.get(0).questionType).isEqualTo(1);
        assertThat(blocks.get(1).questionType).isEqualTo(3);
    }

    @Test
    void parseBlocks_multiLineContent_joinedWithNewline() {
        List<QuestionBlock> blocks = DocxImportServiceImpl.parseBlocks(lines(
                "[主观题]",
                "第一行",
                "第二行",
                "第三行"
        ));
        assertThat(blocks.get(0).content).isEqualTo("第一行\n第二行\n第三行");
    }

    @Test
    void parseBlocks_unknownType_markedAsMinusOne() {
        List<QuestionBlock> blocks = DocxImportServiceImpl.parseBlocks(lines(
                "[未知题型]",
                "题干",
                "答案：A"
        ));
        assertThat(blocks.get(0).questionType).isEqualTo(-1);
    }

    @Test
    void parseBlocks_contentBeforeFirstType_isIgnored() {
        List<QuestionBlock> blocks = DocxImportServiceImpl.parseBlocks(lines(
                "这是文档标题，应被忽略",
                "",
                "[单选题]",
                "真正的题干",
                "A. A",
                "答案：A"
        ));
        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).content).isEqualTo("真正的题干");
    }

    @Test
    void parseBlocks_optionWithFullWidthPunctuation() {
        // A、选项A（中文顿号）
        List<QuestionBlock> blocks = DocxImportServiceImpl.parseBlocks(lines(
                "[单选题]",
                "题干",
                "A、选项A",
                "B、选项B",
                "答案：A"
        ));
        assertThat(blocks.get(0).options).hasSize(2);
        assertThat(blocks.get(0).options.get(0).label).isEqualTo("A");
    }

    // ── importFromDocx 集成流程测试（mock DB，不读真实文件）─────────────────

    @Test
    void importFromDocx_success_andFail_returnsMixedResult() {
        // 用 spy 覆盖 readDocxLines，直接注入测试行
        DocxImportServiceImpl spyService = spy(service);
        doReturn(lines(
                "[单选题]",
                "合法题目",
                "A. 选项A",
                "B. 选项B",
                "答案：A",
                "",
                "[单选题]",
                "无选项的单选题（非法）",
                "答案：A"
        )).when(spyService).readDocxLines(any());

        when(questionMapper.insert((cn.smu.edu.exam.domain.entity.Question) any())).thenReturn(1);
        when(questionOptionMapper.insert((cn.smu.edu.exam.domain.entity.QuestionOption) any())).thenReturn(1);

        DocxImportResultVO result = spyService.importFromDocx(
                1L, null, 99L, new BigDecimal("2.00"));

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailCount()).isEqualTo(1);
        assertThat(result.getDetails().get(0).getSuccess()).isTrue();
        assertThat(result.getDetails().get(1).getSuccess()).isFalse();
    }

    @Test
    void importFromDocx_insertsOptionsWithCorrectMark() {
        DocxImportServiceImpl spyService = spy(service);
        doReturn(lines(
                "[单选题]",
                "题干",
                "A. 选项A",
                "B. 选项B",
                "C. 选项C",
                "答案：B"
        )).when(spyService).readDocxLines(any());

        when(questionMapper.insert((cn.smu.edu.exam.domain.entity.Question) any())).thenReturn(1);

        var optionCaptor = org.mockito.ArgumentCaptor.forClass(
                cn.smu.edu.exam.domain.entity.QuestionOption.class);
        when(questionOptionMapper.insert(optionCaptor.capture())).thenReturn(1);

        spyService.importFromDocx(1L, null, 99L, new BigDecimal("2.00"));

        List<cn.smu.edu.exam.domain.entity.QuestionOption> opts = optionCaptor.getAllValues();
        assertThat(opts).hasSize(3);
        // B is correct
        assertThat(opts.get(0).getIsCorrect()).isEqualTo(0); // A
        assertThat(opts.get(1).getIsCorrect()).isEqualTo(1); // B ✓
        assertThat(opts.get(2).getIsCorrect()).isEqualTo(0); // C
    }

    @Test
    void importFromDocx_multiChoiceOptions_markedCorrectly() {
        DocxImportServiceImpl spyService = spy(service);
        doReturn(lines(
                "[多选题]",
                "题干",
                "A. 选项A",
                "B. 选项B",
                "C. 选项C",
                "D. 选项D",
                "答案：A,C"
        )).when(spyService).readDocxLines(any());

        when(questionMapper.insert((cn.smu.edu.exam.domain.entity.Question) any())).thenReturn(1);

        var optionCaptor = org.mockito.ArgumentCaptor.forClass(
                cn.smu.edu.exam.domain.entity.QuestionOption.class);
        when(questionOptionMapper.insert(optionCaptor.capture())).thenReturn(1);

        spyService.importFromDocx(1L, null, 99L, new BigDecimal("2.00"));

        List<cn.smu.edu.exam.domain.entity.QuestionOption> opts = optionCaptor.getAllValues();
        assertThat(opts.get(0).getIsCorrect()).isEqualTo(1); // A ✓
        assertThat(opts.get(1).getIsCorrect()).isEqualTo(0); // B
        assertThat(opts.get(2).getIsCorrect()).isEqualTo(1); // C ✓
        assertThat(opts.get(3).getIsCorrect()).isEqualTo(0); // D
    }

    @Test
    void importFromDocx_emptyFile_returnsZeroTotal() {
        DocxImportServiceImpl spyService = spy(service);
        doReturn(List.of()).when(spyService).readDocxLines(any());

        DocxImportResultVO result = spyService.importFromDocx(
                1L, null, 99L, new BigDecimal("2.00"));

        assertThat(result.getTotal()).isEqualTo(0);
        assertThat(result.getSuccessCount()).isEqualTo(0);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private List<String> lines(String... lineArray) {
        return Arrays.asList(lineArray);
    }
}
