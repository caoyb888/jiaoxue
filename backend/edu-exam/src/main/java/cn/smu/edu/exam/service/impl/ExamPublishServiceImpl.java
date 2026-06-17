package cn.smu.edu.exam.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.exam.converter.ExamPublishConverter;
import cn.smu.edu.exam.converter.QuestionConverter;
import cn.smu.edu.exam.domain.dto.*;
import cn.smu.edu.exam.domain.entity.*;
import cn.smu.edu.exam.domain.vo.*;
import cn.smu.edu.exam.repository.*;
import cn.smu.edu.exam.service.ExamPublishService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamPublishServiceImpl implements ExamPublishService {

    private static final List<Integer> OPTION_TYPES = List.of(1, 2, 3, 6);

    private final ExamPublishMapper publishMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamPaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final ExamPublishConverter converter;
    private final QuestionConverter questionConverter;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public ExamPublishVO publish(ExamPublishCreateDTO dto, Long teacherId) {
        // 验证时间合法性
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "考试截止时间必须晚于开始时间");
        }

        // 验证试卷存在
        if (paperMapper.selectById(dto.getPaperId()) == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }

        ExamPublish publish = converter.toEntity(dto);
        publish.setTeacherId(teacherId);
        publish.setStatus(computeStatus(dto.getStartTime(), dto.getEndTime()));

        if (StringUtils.hasText(dto.getPassword())) {
            publish.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }

        publishMapper.insert(publish);
        log.info("考试发布成功: publishId={}, paperId={}, classId={}, teacherId={}",
                publish.getId(), dto.getPaperId(), dto.getClassId(), teacherId);
        return toVO(publish);
    }

    @Override
    public ExamPublishVO update(Long publishId, ExamPublishUpdateDTO dto, Long teacherId) {
        ExamPublish publish = requireTeacher(publishId, teacherId);

        // 进行中或已结束的考试不允许修改
        if (publish.getStatus() == 1 || publish.getStatus() == 2) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "考试进行中或已结束，无法修改配置");
        }

        converter.updateEntity(dto, publish);

        // 密码处理：null=不修改；空串=清除密码；非空=更新密码
        if (dto.getPassword() != null) {
            publish.setPasswordHash(
                    dto.getPassword().isEmpty() ? null : passwordEncoder.encode(dto.getPassword()));
        }

        // 重新计算状态
        LocalDateTime start = publish.getStartTime();
        LocalDateTime end   = publish.getEndTime();
        if (start != null && end != null) {
            publish.setStatus(computeStatus(start, end));
        }

        publishMapper.updateById(publish);
        log.info("考试发布更新成功: publishId={}, teacherId={}", publishId, teacherId);
        return toVO(publish);
    }

    @Override
    @Transactional
    public void cancel(Long publishId, Long teacherId) {
        ExamPublish publish = requireTeacher(publishId, teacherId);
        if (publish.getStatus() == 1) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "考试进行中，不可取消");
        }
        publish.setStatus(3);
        publishMapper.updateById(publish);
        publishMapper.deleteById(publishId); // 逻辑删除
        log.info("考试取消成功: publishId={}, teacherId={}", publishId, teacherId);
    }

    @Override
    public ExamPublishVO getById(Long publishId, Long teacherId) {
        ExamPublish publish = requireTeacher(publishId, teacherId);
        return toVO(publish);
    }

    @Override
    public PageResult<ExamPublishVO> listByTeacher(ExamPublishQueryDTO query, Long teacherId) {
        LambdaQueryWrapper<ExamPublish> wrapper = new LambdaQueryWrapper<ExamPublish>()
                .eq(ExamPublish::getTeacherId, teacherId)
                .eq(query.getClassId() != null, ExamPublish::getClassId, query.getClassId())
                .eq(query.getStatus() != null, ExamPublish::getStatus, query.getStatus())
                .orderByDesc(ExamPublish::getStartTime);

        Page<ExamPublish> page = publishMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);

        List<ExamPublishVO> vos = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(vos, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public ExamPublishStudentVO getStudentView(Long publishId, Long studentId, String password) {
        ExamPublish publish = requireExists(publishId);

        // 密码校验（设有密码时必须提供）
        if (StringUtils.hasText(publish.getPasswordHash())) {
            if (!StringUtils.hasText(password) || !passwordEncoder.matches(password, publish.getPasswordHash())) {
                throw new BizException(ErrorCode.EXAM_PASSWORD_WRONG);
            }
        }

        // 考试状态：取消或尚未开始则不允许进入
        int status = computeStatus(publish.getStartTime(), publish.getEndTime());
        if (status == 3) {
            throw new BizException(ErrorCode.EXAM_ALREADY_ENDED);
        }

        boolean answerVisible = publish.getAnswerShowAt() == null
                || LocalDateTime.now().isAfter(publish.getAnswerShowAt());

        List<PaperQuestionVO> questions = loadPaperQuestions(publish.getPaperId(), answerVisible);

        ExamPublishStudentVO vo = new ExamPublishStudentVO();
        vo.setId(publish.getId());
        vo.setPaperId(publish.getPaperId());
        vo.setStartTime(publish.getStartTime());
        vo.setEndTime(publish.getEndTime());
        vo.setDurationMin(publish.getDurationMin());
        vo.setHasPassword(StringUtils.hasText(publish.getPasswordHash()));
        vo.setEnableMonitor(publish.getEnableMonitor());
        vo.setFaceVerifyType(publish.getFaceVerifyType());
        vo.setAllowCopy(publish.getAllowCopy());
        vo.setShuffleQuestion(publish.getShuffleQuestion());
        vo.setShuffleOption(publish.getShuffleOption());
        vo.setStatus(status);
        vo.setStatusLabel(statusLabel(status));
        vo.setAnswerVisible(answerVisible);
        vo.setQuestions(questions);
        return vo;
    }

    @Override
    public boolean verifyPassword(Long publishId, String password) {
        ExamPublish publish = requireExists(publishId);
        if (!StringUtils.hasText(publish.getPasswordHash())) {
            return true;
        }
        return passwordEncoder.matches(password, publish.getPasswordHash());
    }

    // ── private helpers ──────────────────────────────────────────────────────

    /** 实时计算状态（不依赖 DB 冗余字段，避免定时任务延迟问题；start/end 为 null 时返回未开始） */
    public static int computeStatus(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0;
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(start)) return 0;
        if (now.isAfter(end))   return 2;
        return 1;
    }

    public static String statusLabel(int status) {
        return switch (status) {
            case 0 -> "未开始";
            case 1 -> "进行中";
            case 2 -> "已结束";
            case 3 -> "已取消";
            default -> "未知";
        };
    }

    private ExamPublish requireExists(Long publishId) {
        ExamPublish publish = publishMapper.selectById(publishId);
        if (publish == null) {
            throw new BizException(ErrorCode.EXAM_NOT_FOUND);
        }
        return publish;
    }

    private ExamPublish requireTeacher(Long publishId, Long teacherId) {
        ExamPublish publish = requireExists(publishId);
        if (!teacherId.equals(publish.getTeacherId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return publish;
    }

    private ExamPublishVO toVO(ExamPublish publish) {
        ExamPublishVO vo = converter.toVO(publish);
        vo.setHasPassword(StringUtils.hasText(publish.getPasswordHash()));
        int status = computeStatus(publish.getStartTime(), publish.getEndTime());
        vo.setStatus(status);
        vo.setStatusLabel(statusLabel(status));
        return vo;
    }

    /** 批量加载试卷题目（避免 N+1，answerVisible=false 时清空 answer/analysis） */
    private List<PaperQuestionVO> loadPaperQuestions(Long paperId, boolean answerVisible) {
        List<ExamPaperQuestion> relations = paperQuestionMapper.selectByPaperId(paperId);
        if (relations.isEmpty()) return Collections.emptyList();

        Set<Long> questionIds = relations.stream()
                .map(ExamPaperQuestion::getQuestionId)
                .collect(Collectors.toSet());

        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

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

        return relations.stream().map(rel -> {
            PaperQuestionVO pqVO = new PaperQuestionVO();
            pqVO.setId(rel.getId());
            pqVO.setPaperId(rel.getPaperId());
            pqVO.setQuestionId(rel.getQuestionId());
            pqVO.setScore(rel.getScore());
            pqVO.setSortOrder(rel.getSortOrder());
            pqVO.setPaperGroup(rel.getPaperGroup());
            pqVO.setSection(rel.getSection());

            Question q = questionMap.get(rel.getQuestionId());
            if (q != null) {
                QuestionVO qVO = questionConverter.toVO(q);
                if (!answerVisible) {
                    // 隐藏答案字段（C6类约束：答案公布时间未到）
                    qVO.setAnswer(null);
                    qVO.setAnalysis(null);
                }
                if (OPTION_TYPES.contains(q.getType())) {
                    List<QuestionOptionVO> opts = optionsMap.getOrDefault(q.getId(), List.of())
                            .stream().map(opt -> {
                                QuestionOptionVO optVO = questionConverter.toOptionVO(opt);
                                if (!answerVisible) {
                                    optVO.setIsCorrect(null); // 隐藏正确答案标记
                                }
                                return optVO;
                            }).collect(Collectors.toList());
                    qVO.setOptions(opts);
                }
                pqVO.setQuestion(qVO);
            }
            return pqVO;
        }).collect(Collectors.toList());
    }
}
