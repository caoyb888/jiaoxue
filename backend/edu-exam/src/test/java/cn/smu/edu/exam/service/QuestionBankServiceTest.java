package cn.smu.edu.exam.service;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.exam.converter.QuestionBankConverter;
import cn.smu.edu.exam.domain.dto.QuestionBankCreateDTO;
import cn.smu.edu.exam.domain.dto.QuestionBankQueryDTO;
import cn.smu.edu.exam.domain.dto.QuestionBankUpdateDTO;
import cn.smu.edu.exam.domain.entity.QuestionBank;
import cn.smu.edu.exam.domain.vo.QuestionBankVO;
import cn.smu.edu.exam.repository.QuestionBankMapper;
import cn.smu.edu.exam.service.impl.QuestionBankServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionBankServiceTest {

    @Mock
    private QuestionBankMapper questionBankMapper;

    @Mock
    private QuestionBankConverter converter;

    @InjectMocks
    private QuestionBankServiceImpl service;

    private static final Long TEACHER_ID = 1L;
    private static final Long OTHER_TEACHER_ID = 2L;
    private static final Long DEPT_ID = 10L;
    private static final Long BANK_ID = 100L;

    private QuestionBank privateBank;
    private QuestionBank publicBank;

    @BeforeEach
    void setUp() {
        privateBank = new QuestionBank();
        privateBank.setId(BANK_ID);
        privateBank.setTeacherId(TEACHER_ID);
        privateBank.setDeptId(null);
        privateBank.setIsPublic(0);
        privateBank.setCreatedAt(LocalDateTime.now());
        privateBank.setUpdatedAt(LocalDateTime.now());

        publicBank = new QuestionBank();
        publicBank.setId(200L);
        publicBank.setTeacherId(OTHER_TEACHER_ID);
        publicBank.setDeptId(DEPT_ID);
        publicBank.setIsPublic(1);
        publicBank.setCreatedAt(LocalDateTime.now());
        publicBank.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void create_private_shouldSetTeacherIdAndNullDeptId() {
        QuestionBankCreateDTO dto = new QuestionBankCreateDTO();
        dto.setBankName("测试私有题库");
        dto.setIsPublic(0);

        when(converter.toEntity(dto)).thenReturn(new QuestionBank());
        when(converter.toVO(any())).thenReturn(new QuestionBankVO());

        ArgumentCaptor<QuestionBank> captor1 = ArgumentCaptor.forClass(QuestionBank.class);
        service.create(dto, TEACHER_ID, DEPT_ID);
        verify(questionBankMapper).insert(captor1.capture());
        assertThat(captor1.getValue().getTeacherId()).isEqualTo(TEACHER_ID);
        assertThat(captor1.getValue().getDeptId()).isNull();
    }

    @Test
    void create_public_shouldSetDeptId() {
        QuestionBankCreateDTO dto = new QuestionBankCreateDTO();
        dto.setBankName("院系共享题库");
        dto.setIsPublic(1);

        when(converter.toEntity(dto)).thenReturn(new QuestionBank());
        when(converter.toVO(any())).thenReturn(new QuestionBankVO());

        ArgumentCaptor<QuestionBank> captor2 = ArgumentCaptor.forClass(QuestionBank.class);
        service.create(dto, TEACHER_ID, DEPT_ID);
        verify(questionBankMapper).insert(captor2.capture());
        assertThat(captor2.getValue().getTeacherId()).isEqualTo(TEACHER_ID);
        assertThat(captor2.getValue().getDeptId()).isEqualTo(DEPT_ID);
    }

    @Test
    void delete_shouldSucceed_whenOwner() {
        when(questionBankMapper.selectById(BANK_ID)).thenReturn(privateBank);
        assertThatNoException().isThrownBy(() -> service.delete(BANK_ID, TEACHER_ID));
        verify(questionBankMapper).deleteById(BANK_ID);
    }

    @Test
    void delete_shouldThrowForbidden_whenNotOwner() {
        when(questionBankMapper.selectById(BANK_ID)).thenReturn(privateBank);
        assertThatThrownBy(() -> service.delete(BANK_ID, OTHER_TEACHER_ID))
                .isInstanceOf(BizException.class);
    }

    @Test
    void getById_shouldSucceed_forOwner() {
        when(questionBankMapper.selectById(BANK_ID)).thenReturn(privateBank);
        QuestionBankVO vo = new QuestionBankVO();
        when(converter.toVO(privateBank)).thenReturn(vo);

        QuestionBankVO result = service.getById(BANK_ID, TEACHER_ID, DEPT_ID);
        assertThat(result).isNotNull();
        assertThat(result.getEditable()).isTrue();
    }

    @Test
    void getById_shouldSucceed_forSameDeptPublicBank() {
        when(questionBankMapper.selectById(publicBank.getId())).thenReturn(publicBank);
        QuestionBankVO vo = new QuestionBankVO();
        when(converter.toVO(publicBank)).thenReturn(vo);

        // TEACHER_ID 属于 DEPT_ID，访问 OTHER_TEACHER_ID 创建的公开库
        QuestionBankVO result = service.getById(publicBank.getId(), TEACHER_ID, DEPT_ID);
        assertThat(result).isNotNull();
        assertThat(result.getEditable()).isFalse();
    }

    @Test
    void getById_shouldThrowForbidden_forOtherDeptPublicBank() {
        publicBank.setDeptId(99L); // 不同院系
        when(questionBankMapper.selectById(publicBank.getId())).thenReturn(publicBank);

        assertThatThrownBy(() -> service.getById(publicBank.getId(), TEACHER_ID, DEPT_ID))
                .isInstanceOf(BizException.class);
    }

    @Test
    void update_shouldSucceed_whenOwner() {
        when(questionBankMapper.selectById(BANK_ID)).thenReturn(privateBank);
        QuestionBankVO vo = new QuestionBankVO();
        when(converter.toVO(any())).thenReturn(vo);
        doNothing().when(converter).updateEntity(any(), any());

        QuestionBankUpdateDTO dto = new QuestionBankUpdateDTO();
        dto.setBankName("新名称");

        assertThatNoException().isThrownBy(() -> service.update(BANK_ID, dto, TEACHER_ID));
        verify(questionBankMapper).updateById(privateBank);
    }

    @Test
    void update_shouldThrowForbidden_whenNotOwner() {
        when(questionBankMapper.selectById(BANK_ID)).thenReturn(privateBank);
        assertThatThrownBy(() -> service.update(BANK_ID, new QuestionBankUpdateDTO(), OTHER_TEACHER_ID))
                .isInstanceOf(BizException.class);
    }
}
