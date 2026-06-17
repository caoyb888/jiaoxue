package cn.smu.edu.exam.service;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.exam.converter.QuestionConverter;
import cn.smu.edu.exam.domain.dto.QuestionCreateDTO;
import cn.smu.edu.exam.domain.dto.QuestionOptionDTO;
import cn.smu.edu.exam.domain.entity.Question;
import cn.smu.edu.exam.domain.entity.QuestionBank;
import cn.smu.edu.exam.domain.entity.QuestionOption;
import cn.smu.edu.exam.domain.vo.QuestionVO;
import cn.smu.edu.exam.repository.QuestionBankMapper;
import cn.smu.edu.exam.repository.QuestionMapper;
import cn.smu.edu.exam.repository.QuestionOptionMapper;
import cn.smu.edu.exam.service.impl.QuestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock private QuestionMapper questionMapper;
    @Mock private QuestionBankMapper questionBankMapper;
    @Mock private QuestionOptionMapper questionOptionMapper;
    @Mock private QuestionConverter converter;

    @InjectMocks
    private QuestionServiceImpl service;

    private static final Long TEACHER_ID = 1L;
    private static final Long OTHER_TEACHER_ID = 2L;
    private static final Long DEPT_ID = 10L;
    private static final Long BANK_ID = 100L;
    private static final Long QUESTION_ID = 200L;

    private QuestionBank ownBank;
    private Question singleChoiceQuestion;

    @BeforeEach
    void setUp() {
        ownBank = new QuestionBank();
        ownBank.setId(BANK_ID);
        ownBank.setTeacherId(TEACHER_ID);
        ownBank.setIsPublic(0);

        singleChoiceQuestion = new Question();
        singleChoiceQuestion.setId(QUESTION_ID);
        singleChoiceQuestion.setBankId(BANK_ID);
        singleChoiceQuestion.setType(1);
        singleChoiceQuestion.setCreatorId(TEACHER_ID);
        singleChoiceQuestion.setScore(new BigDecimal("2.00"));
    }

    @Test
    void create_singleChoice_shouldInsertQuestionAndOptions() {
        QuestionOptionDTO optA = new QuestionOptionDTO();
        optA.setOptionLabel("A"); optA.setContent("选项A"); optA.setIsCorrect(1);
        QuestionOptionDTO optB = new QuestionOptionDTO();
        optB.setOptionLabel("B"); optB.setContent("选项B"); optB.setIsCorrect(0);

        QuestionCreateDTO dto = new QuestionCreateDTO();
        dto.setBankId(BANK_ID);
        dto.setType(1);
        dto.setContent("这是单选题题干");
        dto.setOptions(List.of(optA, optB));

        when(questionBankMapper.selectById(BANK_ID)).thenReturn(ownBank);
        when(converter.toEntity(dto)).thenReturn(singleChoiceQuestion);
        when(converter.toVO(any())).thenReturn(new QuestionVO());
        when(converter.toOptionEntity(any())).thenReturn(new QuestionOption());
        when(questionOptionMapper.selectByQuestionId(any())).thenReturn(List.of());

        service.create(dto, TEACHER_ID, TEACHER_ID, DEPT_ID);

        verify(questionMapper).insert(singleChoiceQuestion);
        verify(questionOptionMapper, times(2)).insert(any(QuestionOption.class));
    }

    @Test
    void create_shouldThrowForbidden_whenNotBankOwner() {
        // OTHER_TEACHER_ID 尝试向 TEACHER_ID 的私有库中添加题目
        when(questionBankMapper.selectById(BANK_ID)).thenReturn(ownBank);

        QuestionCreateDTO dto = new QuestionCreateDTO();
        dto.setBankId(BANK_ID);
        dto.setType(5);

        assertThatThrownBy(() -> service.create(dto, OTHER_TEACHER_ID, OTHER_TEACHER_ID, DEPT_ID))
                .isInstanceOf(BizException.class);
    }

    @Test
    void delete_shouldSucceed_forCreator() {
        when(questionMapper.selectById(QUESTION_ID)).thenReturn(singleChoiceQuestion);
        when(questionBankMapper.selectById(BANK_ID)).thenReturn(ownBank);

        assertThatNoException().isThrownBy(
                () -> service.delete(QUESTION_ID, TEACHER_ID, DEPT_ID));
        verify(questionMapper).deleteById(QUESTION_ID);
        verify(questionOptionMapper).deleteByQuestionId(QUESTION_ID);
    }

    @Test
    void delete_shouldThrowForbidden_forNonCreator() {
        QuestionBank publicBank = new QuestionBank();
        publicBank.setId(BANK_ID);
        publicBank.setTeacherId(TEACHER_ID);
        publicBank.setIsPublic(1);
        publicBank.setDeptId(DEPT_ID);

        singleChoiceQuestion.setCreatorId(TEACHER_ID);
        when(questionMapper.selectById(QUESTION_ID)).thenReturn(singleChoiceQuestion);
        when(questionBankMapper.selectById(BANK_ID)).thenReturn(publicBank);

        // OTHER_TEACHER_ID 属于同院系，可见公开库，但无法删除非自己创建的题目
        assertThatThrownBy(() -> service.delete(QUESTION_ID, OTHER_TEACHER_ID, DEPT_ID))
                .isInstanceOf(BizException.class);
    }
}
