package cn.smu.edu.exam.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.exam.converter.QuestionConverter;
import cn.smu.edu.exam.domain.dto.QuestionCreateDTO;
import cn.smu.edu.exam.domain.dto.QuestionOptionDTO;
import cn.smu.edu.exam.domain.dto.QuestionQueryDTO;
import cn.smu.edu.exam.domain.dto.QuestionUpdateDTO;
import cn.smu.edu.exam.domain.entity.Question;
import cn.smu.edu.exam.domain.entity.QuestionBank;
import cn.smu.edu.exam.domain.entity.QuestionOption;
import cn.smu.edu.exam.domain.vo.QuestionOptionVO;
import cn.smu.edu.exam.domain.vo.QuestionVO;
import cn.smu.edu.exam.repository.QuestionBankMapper;
import cn.smu.edu.exam.repository.QuestionMapper;
import cn.smu.edu.exam.repository.QuestionOptionMapper;
import cn.smu.edu.exam.service.QuestionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** 1/2/3/6 为客观题（有 question_option 选项），4/5 为主观题（仅 answer 字段） */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private static final List<Integer> OPTION_TYPES = List.of(1, 2, 3, 6);

    private final QuestionMapper questionMapper;
    private final QuestionBankMapper questionBankMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final QuestionConverter converter;

    @Override
    @Transactional
    public QuestionVO create(QuestionCreateDTO dto, Long creatorId, Long teacherId, Long deptId) {
        // 验证题库访问权限（只有题库可见才能向其中添加题目）
        QuestionBank bank = requireBankVisible(dto.getBankId(), teacherId, deptId);

        // 只有题库 owner 才能向题库中添加题目
        if (!teacherId.equals(bank.getTeacherId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        Question question = converter.toEntity(dto);
        question.setCreatorId(creatorId);
        questionMapper.insert(question);

        // 客观题插入选项
        if (OPTION_TYPES.contains(dto.getType()) && !CollectionUtils.isEmpty(dto.getOptions())) {
            saveOptions(question.getId(), dto.getOptions());
        }

        log.info("题目创建成功: questionId={}, bankId={}, type={}", question.getId(), dto.getBankId(), dto.getType());
        return buildVO(question);
    }

    @Override
    @Transactional
    public QuestionVO update(Long questionId, QuestionUpdateDTO dto, Long teacherId, Long deptId) {
        Question question = requireQuestionOwner(questionId, teacherId, deptId);
        converter.updateEntity(dto, question);
        questionMapper.updateById(question);

        // 如果传入了 options，全量替换
        if (dto.getOptions() != null) {
            questionOptionMapper.deleteByQuestionId(questionId);
            if (!dto.getOptions().isEmpty() && OPTION_TYPES.contains(question.getType())) {
                saveOptions(questionId, dto.getOptions());
            }
        }

        log.info("题目更新成功: questionId={}, teacherId={}", questionId, teacherId);
        return buildVO(question);
    }

    @Override
    @Transactional
    public void delete(Long questionId, Long teacherId, Long deptId) {
        requireQuestionOwner(questionId, teacherId, deptId);
        questionMapper.deleteById(questionId);
        questionOptionMapper.deleteByQuestionId(questionId);
        log.info("题目删除成功: questionId={}, teacherId={}", questionId, teacherId);
    }

    @Override
    public QuestionVO getById(Long questionId, Long teacherId, Long deptId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        requireBankVisible(question.getBankId(), teacherId, deptId);
        return buildVO(question);
    }

    @Override
    public PageResult<QuestionVO> list(QuestionQueryDTO query, Long teacherId, Long deptId) {
        requireBankVisible(query.getBankId(), teacherId, deptId);

        // 关键词走全文索引，其他条件用 LambdaQueryWrapper
        if (StringUtils.hasText(query.getKeyword())) {
            Page<Question> page = new Page<>(query.getPage(), query.getSize());
            IPage<Question> result = questionMapper.searchByKeyword(page, query.getBankId(), query.getKeyword());
            return toPageResult(result);
        }

        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getBankId, query.getBankId())
                .eq(query.getType() != null, Question::getType, query.getType())
                .eq(query.getDifficulty() != null, Question::getDifficulty, query.getDifficulty())
                .orderByDesc(Question::getCreatedAt);

        Page<Question> page = questionMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);
        return toPageResult(page);
    }

    private void saveOptions(Long questionId, List<QuestionOptionDTO> optionDTOs) {
        List<QuestionOption> options = new ArrayList<>(optionDTOs.size());
        for (int i = 0; i < optionDTOs.size(); i++) {
            QuestionOption opt = converter.toOptionEntity(optionDTOs.get(i));
            opt.setQuestionId(questionId);
            if (opt.getSortOrder() == null) {
                opt.setSortOrder(i + 1);
            }
            options.add(opt);
        }
        // 批量插入
        options.forEach(questionOptionMapper::insert);
    }

    private QuestionVO buildVO(Question question) {
        QuestionVO vo = converter.toVO(question);
        if (OPTION_TYPES.contains(question.getType())) {
            List<QuestionOption> opts = questionOptionMapper.selectByQuestionId(question.getId());
            List<QuestionOptionVO> optVOs = opts.stream()
                    .map(converter::toOptionVO)
                    .collect(Collectors.toList());
            vo.setOptions(optVOs);
        }
        return vo;
    }

    private PageResult<QuestionVO> toPageResult(IPage<Question> page) {
        List<QuestionVO> vos = page.getRecords().stream()
                .map(this::buildVO)
                .collect(Collectors.toList());
        return PageResult.of(vos, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 验证题库对当前教师可见（自己的 或 同院系公开库） */
    private QuestionBank requireBankVisible(Long bankId, Long teacherId, Long deptId) {
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

    /** 验证题目只有创建者可修改/删除 */
    private Question requireQuestionOwner(Long questionId, Long teacherId, Long deptId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        // 验证对所属题库有访问权限
        requireBankVisible(question.getBankId(), teacherId, deptId);
        // 只有题目创建者可以修改/删除
        if (!teacherId.equals(question.getCreatorId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return question;
    }
}
