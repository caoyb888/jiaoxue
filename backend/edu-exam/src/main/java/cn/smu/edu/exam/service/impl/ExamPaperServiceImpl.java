package cn.smu.edu.exam.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.exam.converter.ExamPaperConverter;
import cn.smu.edu.exam.converter.QuestionConverter;
import cn.smu.edu.exam.domain.dto.*;
import cn.smu.edu.exam.domain.entity.ExamPaper;
import cn.smu.edu.exam.domain.entity.ExamPaperQuestion;
import cn.smu.edu.exam.domain.entity.Question;
import cn.smu.edu.exam.domain.entity.QuestionOption;
import cn.smu.edu.exam.domain.vo.*;
import cn.smu.edu.exam.repository.*;
import cn.smu.edu.exam.service.ExamPaperService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamPaperServiceImpl implements ExamPaperService {

    private static final List<Integer> OPTION_TYPES = List.of(1, 2, 3, 6);

    private final ExamPaperMapper examPaperMapper;
    private final ExamPaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final ExamPaperConverter converter;
    private final QuestionConverter questionConverter;

    @Override
    public ExamPaperVO create(ExamPaperCreateDTO dto, Long creatorId) {
        ExamPaper paper = converter.toEntity(dto);
        paper.setCreatorId(creatorId);
        examPaperMapper.insert(paper);
        log.info("试卷创建成功: paperId={}, creatorId={}", paper.getId(), creatorId);
        return toVO(paper, creatorId);
    }

    @Override
    public ExamPaperVO update(Long paperId, ExamPaperUpdateDTO dto, Long creatorId) {
        ExamPaper paper = requireOwner(paperId, creatorId);
        converter.updateEntity(dto, paper);
        examPaperMapper.updateById(paper);
        log.info("试卷更新成功: paperId={}, creatorId={}", paperId, creatorId);
        return toVO(paper, creatorId);
    }

    @Override
    @Transactional
    public void delete(Long paperId, Long creatorId) {
        requireOwner(paperId, creatorId);
        examPaperMapper.deleteById(paperId);
        paperQuestionMapper.deleteAllByPaperId(paperId);
        log.info("试卷删除成功: paperId={}, creatorId={}", paperId, creatorId);
    }

    @Override
    public ExamPaperDetailVO getDetail(Long paperId, Long creatorId) {
        ExamPaper paper = requireOwner(paperId, creatorId);
        return buildDetailVO(paper, creatorId);
    }

    @Override
    public PageResult<ExamPaperVO> list(ExamPaperQueryDTO query, Long creatorId) {
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getCreatorId, creatorId)
                .like(StringUtils.hasText(query.getKeyword()), ExamPaper::getTitle, query.getKeyword())
                .eq(query.getIsRandom() != null, ExamPaper::getIsRandom, query.getIsRandom())
                .orderByDesc(ExamPaper::getCreatedAt);

        Page<ExamPaper> page = examPaperMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);

        List<ExamPaperVO> vos = page.getRecords().stream()
                .map(p -> toVO(p, creatorId))
                .collect(Collectors.toList());
        return PageResult.of(vos, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional
    public ExamPaperDetailVO addQuestions(Long paperId, BatchAddQuestionsDTO dto, Long creatorId) {
        ExamPaper paper = requireOwner(paperId, creatorId);

        for (AddQuestionDTO addDto : dto.getQuestions()) {
            String group = addDto.getPaperGroup() == null ? "A" : addDto.getPaperGroup();

            // 跳过已存在的关联（uk_paper_question_group 约束）
            if (paperQuestionMapper.existsRelation(paperId, addDto.getQuestionId(), group) > 0) {
                log.warn("题目已存在于试卷，跳过: paperId={}, questionId={}, group={}", paperId, addDto.getQuestionId(), group);
                continue;
            }

            // 验证题目存在
            if (questionMapper.selectById(addDto.getQuestionId()) == null) {
                throw new BizException(ErrorCode.NOT_FOUND);
            }

            ExamPaperQuestion rel = new ExamPaperQuestion();
            rel.setPaperId(paperId);
            rel.setQuestionId(addDto.getQuestionId());
            rel.setScore(addDto.getScore());
            rel.setPaperGroup(group);
            rel.setSection(addDto.getSection());
            // 未指定排序则自动追加到末尾
            int nextOrder = addDto.getSortOrder() != null
                    ? addDto.getSortOrder()
                    : paperQuestionMapper.maxSortOrder(paperId, group) + 1;
            rel.setSortOrder(nextOrder);
            paperQuestionMapper.insert(rel);
        }

        log.info("批量添加题目成功: paperId={}, count={}", paperId, dto.getQuestions().size());
        return buildDetailVO(paper, creatorId);
    }

    @Override
    @Transactional
    public void removeQuestion(Long paperId, Long questionId, String paperGroup, Long creatorId) {
        requireOwner(paperId, creatorId);
        String group = paperGroup != null ? paperGroup : "A";
        int deleted = paperQuestionMapper.deleteByPaperAndQuestion(paperId, questionId, group);
        if (deleted == 0) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        log.info("移除题目成功: paperId={}, questionId={}, group={}", paperId, questionId, group);
    }

    @Override
    @Transactional
    public ExamPaperDetailVO randomCompose(Long paperId, RandomCompositionDTO dto, Long creatorId) {
        ExamPaper paper = requireOwner(paperId, creatorId);

        // 清空指定卷组（或全部）
        if (dto.getClearGroup() != null) {
            paperQuestionMapper.deleteByPaperGroup(paperId, dto.getClearGroup());
        } else {
            paperQuestionMapper.deleteAllByPaperId(paperId);
        }

        // 已用题目ID集合（同一卷内不重复）
        Set<Long> usedIds = new HashSet<>();
        int globalSortOrder = 1;

        for (RandomPickRuleDTO rule : dto.getRules()) {
            String group = rule.getPaperGroup() == null ? "A" : rule.getPaperGroup();

            // 查询符合条件的题目（排除本次已使用的）
            LambdaQueryWrapper<Question> qw = new LambdaQueryWrapper<Question>()
                    .eq(Question::getBankId, rule.getBankId())
                    .eq(rule.getType() != null, Question::getType, rule.getType())
                    .eq(rule.getDifficulty() != null, Question::getDifficulty, rule.getDifficulty())
                    .notIn(!usedIds.isEmpty(), Question::getId, usedIds);

            List<Question> candidates = questionMapper.selectList(qw);

            if (candidates.size() < rule.getCount()) {
                throw new BizException(ErrorCode.PARAM_ERROR.getCode(),
                        String.format("题库[%d]符合条件的题目不足 %d 道（仅找到 %d 道）",
                                rule.getBankId(), rule.getCount(), candidates.size()));
            }

            // 随机抽取
            Collections.shuffle(candidates);
            List<Question> picked = candidates.subList(0, rule.getCount());

            for (Question q : picked) {
                ExamPaperQuestion rel = new ExamPaperQuestion();
                rel.setPaperId(paperId);
                rel.setQuestionId(q.getId());
                rel.setScore(rule.getScorePerQuestion());
                rel.setSortOrder(globalSortOrder++);
                rel.setPaperGroup(group);
                rel.setSection(rule.getSection());
                paperQuestionMapper.insert(rel);
                usedIds.add(q.getId());
            }
        }

        log.info("随机组卷完成: paperId={}, rules={}", paperId, dto.getRules().size());
        return buildDetailVO(paper, creatorId);
    }

    @Override
    public ScoreCheckVO checkScore(Long paperId, Long creatorId) {
        ExamPaper paper = requireOwner(paperId, creatorId);
        BigDecimal actual = paperQuestionMapper.sumScoreByPaperId(paperId);
        Long countLong = paperQuestionMapper.selectCount(
                new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, paperId));
        int count = countLong == null ? 0 : countLong.intValue();
        BigDecimal diff = actual.subtract(paper.getTotalScore());
        return new ScoreCheckVO(paper.getTotalScore(), actual, count,
                diff.compareTo(BigDecimal.ZERO) == 0, diff);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private ExamPaper requireOwner(Long paperId, Long creatorId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (!creatorId.equals(paper.getCreatorId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return paper;
    }

    private ExamPaperVO toVO(ExamPaper paper, Long currentUserId) {
        ExamPaperVO vo = converter.toVO(paper);
        vo.setEditable(currentUserId.equals(paper.getCreatorId()));
        return vo;
    }

    private ExamPaperDetailVO buildDetailVO(ExamPaper paper, Long creatorId) {
        List<ExamPaperQuestion> relations = paperQuestionMapper.selectByPaperId(paper.getId());

        // 批量加载题目（避免 N+1）
        Set<Long> questionIds = relations.stream()
                .map(ExamPaperQuestion::getQuestionId)
                .collect(Collectors.toSet());

        Map<Long, Question> questionMap = questionIds.isEmpty()
                ? Collections.emptyMap()
                : questionMapper.selectBatchIds(questionIds).stream()
                        .collect(Collectors.toMap(Question::getId, q -> q));

        // 批量加载选项（客观题）
        Set<Long> objectiveIds = questionMap.values().stream()
                .filter(q -> OPTION_TYPES.contains(q.getType()))
                .map(Question::getId)
                .collect(Collectors.toSet());

        Map<Long, List<QuestionOption>> optionsMap = new HashMap<>();
        if (!objectiveIds.isEmpty()) {
            LambdaQueryWrapper<QuestionOption> optQw = new LambdaQueryWrapper<QuestionOption>()
                    .in(QuestionOption::getQuestionId, objectiveIds)
                    .orderByAsc(QuestionOption::getSortOrder);
            questionOptionMapper.selectList(optQw)
                    .forEach(opt -> optionsMap
                            .computeIfAbsent(opt.getQuestionId(), k -> new ArrayList<>())
                            .add(opt));
        }

        List<PaperQuestionVO> pqVOs = relations.stream().map(rel -> {
            PaperQuestionVO pqVO = converter.toPaperQuestionVO(rel);
            Question q = questionMap.get(rel.getQuestionId());
            if (q != null) {
                QuestionVO qVO = questionConverter.toVO(q);
                if (OPTION_TYPES.contains(q.getType())) {
                    qVO.setOptions(optionsMap.getOrDefault(q.getId(), List.of()).stream()
                            .map(questionConverter::toOptionVO)
                            .collect(Collectors.toList()));
                }
                pqVO.setQuestion(qVO);
            }
            return pqVO;
        }).collect(Collectors.toList());

        BigDecimal actualScore = relations.stream()
                .map(ExamPaperQuestion::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ExamPaperDetailVO detail = new ExamPaperDetailVO();
        // 手动复制 ExamPaperVO 字段（继承 + toVO）
        ExamPaperVO base = toVO(paper, creatorId);
        detail.setId(base.getId());
        detail.setCreatorId(base.getCreatorId());
        detail.setTitle(base.getTitle());
        detail.setTotalScore(base.getTotalScore());
        detail.setIsRandom(base.getIsRandom());
        detail.setPaperType(base.getPaperType());
        detail.setDescription(base.getDescription());
        detail.setCreatedAt(base.getCreatedAt());
        detail.setUpdatedAt(base.getUpdatedAt());
        detail.setEditable(base.getEditable());
        detail.setQuestions(pqVOs);
        detail.setTotalQuestions(pqVOs.size());
        detail.setActualScore(actualScore);
        return detail;
    }
}
