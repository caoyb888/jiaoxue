package cn.smu.edu.exam.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.exam.converter.QuestionBankConverter;
import cn.smu.edu.exam.domain.dto.QuestionBankCreateDTO;
import cn.smu.edu.exam.domain.dto.QuestionBankQueryDTO;
import cn.smu.edu.exam.domain.dto.QuestionBankUpdateDTO;
import cn.smu.edu.exam.domain.entity.QuestionBank;
import cn.smu.edu.exam.domain.vo.QuestionBankVO;
import cn.smu.edu.exam.repository.QuestionBankMapper;
import cn.smu.edu.exam.service.QuestionBankService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl implements QuestionBankService {

    private final QuestionBankMapper questionBankMapper;
    private final QuestionBankConverter converter;

    @Override
    public QuestionBankVO create(QuestionBankCreateDTO dto, Long teacherId, Long deptId) {
        QuestionBank bank = converter.toEntity(dto);
        bank.setTeacherId(teacherId);
        // 院系共享时绑定院系；私有库不绑定
        bank.setDeptId(dto.getIsPublic() == 1 ? deptId : null);
        questionBankMapper.insert(bank);
        log.info("题库创建成功: bankId={}, teacherId={}, isPublic={}", bank.getId(), teacherId, dto.getIsPublic());
        return toVO(bank, teacherId);
    }

    @Override
    public QuestionBankVO update(Long bankId, QuestionBankUpdateDTO dto, Long teacherId) {
        QuestionBank bank = requireOwner(bankId, teacherId);
        converter.updateEntity(dto, bank);
        // 如果切换为院系共享，deptId 保持不变（创建时已设置）；若私有化则清空 deptId
        if (dto.getIsPublic() != null && dto.getIsPublic() == 0) {
            bank.setDeptId(null);
        }
        questionBankMapper.updateById(bank);
        log.info("题库更新成功: bankId={}, teacherId={}", bankId, teacherId);
        return toVO(bank, teacherId);
    }

    @Override
    public void delete(Long bankId, Long teacherId) {
        requireOwner(bankId, teacherId);
        questionBankMapper.deleteById(bankId);
        log.info("题库删除成功: bankId={}, teacherId={}", bankId, teacherId);
    }

    @Override
    public QuestionBankVO getById(Long bankId, Long teacherId, Long deptId) {
        QuestionBank bank = requireVisible(bankId, teacherId, deptId);
        return toVO(bank, teacherId);
    }

    @Override
    public PageResult<QuestionBankVO> list(QuestionBankQueryDTO query, Long teacherId, Long deptId) {
        LambdaQueryWrapper<QuestionBank> wrapper = new LambdaQueryWrapper<QuestionBank>()
                .and(w -> w
                        // 自己的所有题库
                        .eq(QuestionBank::getTeacherId, teacherId)
                        // 或者：同院系的公开题库（不是自己创建的）
                        .or(deptId != null, sub -> sub
                                .eq(QuestionBank::getIsPublic, 1)
                                .eq(QuestionBank::getDeptId, deptId)
                                .ne(QuestionBank::getTeacherId, teacherId)
                        )
                );

        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(QuestionBank::getBankName, query.getKeyword());
        }
        if (query.getIsPublic() != null) {
            wrapper.eq(QuestionBank::getIsPublic, query.getIsPublic());
        }
        wrapper.orderByDesc(QuestionBank::getCreatedAt);

        Page<QuestionBank> page = questionBankMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);

        List<QuestionBankVO> vos = page.getRecords().stream()
                .map(b -> toVO(b, teacherId))
                .collect(Collectors.toList());
        return PageResult.of(vos, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 断言当前用户是题库 owner，否则抛403 */
    private QuestionBank requireOwner(Long bankId, Long teacherId) {
        QuestionBank bank = questionBankMapper.selectById(bankId);
        if (bank == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (!teacherId.equals(bank.getTeacherId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return bank;
    }

    /** 断言当前用户对题库有可见权限（自己的 或 同院系公开库） */
    private QuestionBank requireVisible(Long bankId, Long teacherId, Long deptId) {
        QuestionBank bank = questionBankMapper.selectById(bankId);
        if (bank == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        boolean isOwner = teacherId.equals(bank.getTeacherId());
        boolean isDeptPublic = bank.getIsPublic() == 1
                && deptId != null
                && deptId.equals(bank.getDeptId());
        if (!isOwner && !isDeptPublic) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return bank;
    }

    private QuestionBankVO toVO(QuestionBank bank, Long currentTeacherId) {
        QuestionBankVO vo = converter.toVO(bank);
        vo.setEditable(currentTeacherId.equals(bank.getTeacherId()));
        return vo;
    }
}
