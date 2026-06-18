package cn.smu.edu.exam.controller;

import cn.smu.edu.common.exception.GlobalExceptionHandler;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.common.util.UserContext;
import cn.smu.edu.exam.domain.dto.QuestionBankCreateDTO;
import cn.smu.edu.exam.domain.vo.QuestionBankVO;
import cn.smu.edu.exam.domain.vo.QuestionOptionVO;
import cn.smu.edu.exam.domain.vo.QuestionVO;
import cn.smu.edu.exam.service.QuestionBankService;
import cn.smu.edu.exam.service.QuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * S4-14 题目 CRUD 接口自动化测试
 * 覆盖题库创建 → 题目创建 → 题目查询 → 题目更新 → 题目删除完整链路
 * 使用 standalone MockMvc，避免 @WebMvcTest AOP 依赖问题
 */
@ExtendWith(MockitoExtension.class)
class QuestionCrudChainTest {

    MockMvc mockMvc;
    final ObjectMapper mapper = new ObjectMapper();

    @Mock QuestionBankService questionBankService;
    @Mock QuestionService questionService;

    private static final Long TEACHER_ID = 1L;
    private static final Long DEPT_ID    = 10L;
    private static final Long BANK_ID    = 100L;
    private static final Long Q_ID       = 200L;

    @BeforeEach
    void setUp() {
        UserContext.setUserId(TEACHER_ID);
        UserContext.setDeptId(DEPT_ID);
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new QuestionBankController(questionBankService),
                        new QuestionController(questionService)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── 题库 CRUD ─────────────────────────────────────────────────────────────

    @Test
    void createBank_shouldReturn200_whenValidRequest() throws Exception {
        QuestionBankVO vo = buildBankVO();
        when(questionBankService.create(any(), eq(TEACHER_ID), eq(DEPT_ID))).thenReturn(vo);

        QuestionBankCreateDTO dto = new QuestionBankCreateDTO();
        dto.setBankName("Java程序设计题库");
        dto.setIsPublic(0);

        mockMvc.perform(post("/api/v1/exam/banks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.bankName").value("Java程序设计题库"));
    }

    @Test
    void createBank_shouldReturn400_whenBankNameBlank() throws Exception {
        QuestionBankCreateDTO dto = new QuestionBankCreateDTO();
        dto.setBankName("");

        mockMvc.perform(post("/api/v1/exam/banks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listBanks_shouldReturn200_withPageResult() throws Exception {
        PageResult<QuestionBankVO> page = new PageResult<>(List.of(buildBankVO()), 1L, 1L, 20L, 1L);
        when(questionBankService.list(any(), eq(TEACHER_ID), eq(DEPT_ID))).thenReturn(page);

        mockMvc.perform(get("/api/v1/exam/banks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(1L));
    }

    @Test
    void deleteBank_shouldReturn200_whenOwner() throws Exception {
        doNothing().when(questionBankService).delete(BANK_ID, TEACHER_ID);

        mockMvc.perform(delete("/api/v1/exam/banks/" + BANK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(questionBankService).delete(BANK_ID, TEACHER_ID);
    }

    // ── 题目 CRUD ─────────────────────────────────────────────────────────────

    @Test
    void createSingleChoiceQuestion_shouldReturn200() throws Exception {
        QuestionVO vo = buildSingleChoiceVO();
        when(questionService.create(any(), eq(TEACHER_ID), eq(TEACHER_ID), eq(DEPT_ID))).thenReturn(vo);

        String body = """
                {
                  "bankId": 100,
                  "type": 1,
                  "content": "Java 中哪种类型是基本类型？",
                  "answer": "A",
                  "score": 2.0,
                  "options": [
                    {"optionLabel": "A", "content": "int",    "isCorrect": 1},
                    {"optionLabel": "B", "content": "String", "isCorrect": 0},
                    {"optionLabel": "C", "content": "List",   "isCorrect": 0},
                    {"optionLabel": "D", "content": "Map",    "isCorrect": 0}
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/exam/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.type").value(1))
                .andExpect(jsonPath("$.data.options").isArray());
    }

    @Test
    void createQuestion_shouldReturn400_whenContentBlank() throws Exception {
        String body = """
                {
                  "bankId": 100,
                  "type": 1,
                  "content": "",
                  "score": 2.0
                }
                """;

        mockMvc.perform(post("/api/v1/exam/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createQuestion_shouldReturn400_whenTypeOutOfRange() throws Exception {
        String body = """
                {
                  "bankId": 100,
                  "type": 99,
                  "content": "合法题干",
                  "score": 2.0
                }
                """;

        mockMvc.perform(post("/api/v1/exam/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createQuestion_shouldReturn400_whenScoreNegative() throws Exception {
        String body = """
                {
                  "bankId": 100,
                  "type": 3,
                  "content": "Java是面向对象语言",
                  "score": -1
                }
                """;

        mockMvc.perform(post("/api/v1/exam/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getQuestionById_shouldReturn200() throws Exception {
        QuestionVO vo = buildSingleChoiceVO();
        when(questionService.getById(Q_ID, TEACHER_ID, DEPT_ID)).thenReturn(vo);

        mockMvc.perform(get("/api/v1/exam/questions/" + Q_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(Q_ID.intValue()));
    }

    @Test
    void listQuestions_shouldReturn200() throws Exception {
        PageResult<QuestionVO> page = new PageResult<>(List.of(buildSingleChoiceVO()), 1L, 1L, 20L, 1L);
        when(questionService.list(any(), eq(TEACHER_ID), eq(DEPT_ID))).thenReturn(page);

        mockMvc.perform(get("/api/v1/exam/questions").param("bankId", BANK_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].id").value(Q_ID.intValue()));
    }

    @Test
    void deleteQuestion_shouldReturn200() throws Exception {
        doNothing().when(questionService).delete(Q_ID, TEACHER_ID, DEPT_ID);

        mockMvc.perform(delete("/api/v1/exam/questions/" + Q_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(questionService).delete(Q_ID, TEACHER_ID, DEPT_ID);
    }

    // ── 链路验证：创建题库 → 创建题目 → 查询 → 删除 ──────────────────────────

    @Test
    void fullCrudChain_bank_then_question_then_delete() throws Exception {
        // Step 1: 创建题库
        when(questionBankService.create(any(), eq(TEACHER_ID), eq(DEPT_ID))).thenReturn(buildBankVO());

        QuestionBankCreateDTO bankDTO = new QuestionBankCreateDTO();
        bankDTO.setBankName("链路测试题库");
        bankDTO.setIsPublic(0);

        mockMvc.perform(post("/api/v1/exam/banks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(bankDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(BANK_ID.intValue()));

        // Step 2: 在题库中创建题目
        when(questionService.create(any(), eq(TEACHER_ID), eq(TEACHER_ID), eq(DEPT_ID))).thenReturn(buildSingleChoiceVO());

        String qBody = """
                {
                  "bankId": 100,
                  "type": 1,
                  "content": "链路测试题目",
                  "answer": "A",
                  "score": 5.0,
                  "options": [
                    {"optionLabel": "A", "content": "选项A", "isCorrect": 1},
                    {"optionLabel": "B", "content": "选项B", "isCorrect": 0}
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/exam/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(qBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(Q_ID.intValue()));

        // Step 3: 查询题目
        when(questionService.getById(Q_ID, TEACHER_ID, DEPT_ID)).thenReturn(buildSingleChoiceVO());

        mockMvc.perform(get("/api/v1/exam/questions/" + Q_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bankId").value(BANK_ID.intValue()));

        // Step 4: 删除题目
        doNothing().when(questionService).delete(Q_ID, TEACHER_ID, DEPT_ID);

        mockMvc.perform(delete("/api/v1/exam/questions/" + Q_ID))
                .andExpect(status().isOk());

        // Step 5: 删除题库
        doNothing().when(questionBankService).delete(BANK_ID, TEACHER_ID);

        mockMvc.perform(delete("/api/v1/exam/banks/" + BANK_ID))
                .andExpect(status().isOk());

        verify(questionService).delete(Q_ID, TEACHER_ID, DEPT_ID);
        verify(questionBankService).delete(BANK_ID, TEACHER_ID);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private QuestionBankVO buildBankVO() {
        QuestionBankVO vo = new QuestionBankVO();
        vo.setId(BANK_ID);
        vo.setTeacherId(TEACHER_ID);
        vo.setDeptId(DEPT_ID);
        vo.setBankName("Java程序设计题库");
        vo.setIsPublic(0);
        vo.setEditable(true);
        return vo;
    }

    private QuestionVO buildSingleChoiceVO() {
        QuestionVO vo = new QuestionVO();
        vo.setId(Q_ID);
        vo.setBankId(BANK_ID);
        vo.setType(1);
        vo.setContent("Java 中哪种类型是基本类型？");
        vo.setAnswer("A");
        vo.setScore(new BigDecimal("2.00"));
        vo.setDifficulty(3);
        vo.setCreatorId(TEACHER_ID);

        QuestionOptionVO optA = new QuestionOptionVO();
        optA.setId(1L); optA.setOptionLabel("A"); optA.setContent("int"); optA.setIsCorrect(1);
        QuestionOptionVO optB = new QuestionOptionVO();
        optB.setId(2L); optB.setOptionLabel("B"); optB.setContent("String"); optB.setIsCorrect(0);
        vo.setOptions(List.of(optA, optB));
        return vo;
    }
}
