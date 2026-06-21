package cn.smu.edu.exam.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.exam.converter.QuestionConverter;
import cn.smu.edu.exam.domain.dto.ExamEnterDTO;
import cn.smu.edu.exam.domain.entity.*;
import cn.smu.edu.exam.domain.vo.*;
import cn.smu.edu.exam.repository.*;
import cn.smu.edu.exam.service.ExamEnterService;
import cn.smu.edu.exam.service.impl.ExamPublishServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
public class ExamEnterServiceImpl implements ExamEnterService {

    static final int PAGE_SIZE = 10;

    private final ExamPublishMapper publishMapper;
    private final ExamMonitorMapper monitorMapper;
    private final ExamPaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final QuestionConverter questionConverter;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ExamEnterVO enter(Long publishId, Long studentId, ExamEnterDTO dto) {
        ExamPublish publish = requireActiveExam(publishId);

        // 密码验证
        if (StringUtils.hasText(publish.getPasswordHash())) {
            String pw = dto != null ? dto.getPassword() : null;
            if (!StringUtils.hasText(pw) || !passwordEncoder.matches(pw, publish.getPasswordHash())) {
                throw new BizException(ErrorCode.EXAM_PASSWORD_WRONG);
            }
        }

        // 查找或创建监考记录
        ExamMonitor monitor = monitorMapper.selectByPublishAndStudent(publishId, studentId);
        boolean firstEnter = monitor == null;

        if (firstEnter) {
            monitor = new ExamMonitor();
            monitor.setPublishId(publishId);
            monitor.setStudentId(studentId);
            // 需要人脸核验时先进入 VERIFYING 状态
            monitor.setSessionStatus(publish.getFaceVerifyType() != null && publish.getFaceVerifyType() > 0
                    ? "VERIFYING" : "ANSWERING");
            monitor.setTabSwitchCount(0);
            monitor.setScreenshotCount(0);
            monitor.setCopyCount(0);
            monitor.setAbnormalFlag(0);
            monitor.setLastHeartbeatAt(LocalDateTime.now());
            monitorMapper.insert(monitor);
            log.info("学生进入考试: publishId={}, studentId={}, status={}", publishId, studentId, monitor.getSessionStatus());
        } else if ("SUBMITTED".equals(monitor.getSessionStatus())) {
            throw new BizException(ErrorCode.EXAM_ALREADY_SUBMITTED);
        } else {
            // 重连：更新心跳时间
            monitor.setLastHeartbeatAt(LocalDateTime.now());
            monitorMapper.updateById(monitor);
            log.info("学生重连考试: publishId={}, studentId={}, status={}", publishId, studentId, monitor.getSessionStatus());
        }

        // 获取第一页题目（考试中始终不显示答案）
        List<PaperQuestionVO> firstPage = loadPagedQuestions(publish, studentId, 1);
        int total = countQuestions(publish.getPaperId());

        ExamEnterVO vo = new ExamEnterVO();
        vo.setPublishId(publish.getId());
        vo.setPaperId(publish.getPaperId());
        vo.setStartTime(publish.getStartTime());
        vo.setEndTime(publish.getEndTime());
        vo.setDurationMin(publish.getDurationMin());
        vo.setEnableMonitor(publish.getEnableMonitor());
        vo.setFaceVerifyType(publish.getFaceVerifyType());
        vo.setAllowCopy(publish.getAllowCopy());
        vo.setShuffleQuestion(publish.getShuffleQuestion());
        vo.setShuffleOption(publish.getShuffleOption());
        vo.setSessionStatus(monitor.getSessionStatus());
        vo.setFaceVerifyPassed(monitor.getFaceVerifyPassed());
        vo.setFirstEnter(firstEnter);
        vo.setQuestions(firstPage);
        vo.setTotalQuestions(total);
        vo.setTotalPages((int) Math.ceil((double) total / PAGE_SIZE));
        vo.setCurrentPage(1);
        return vo;
    }

    @Override
    public ExamQuestionPageVO getQuestionsPage(Long publishId, Long studentId, int page) {
        ExamPublish publish = requireActiveExam(publishId);

        // 必须先调用 enter 建立监考记录
        ExamMonitor monitor = monitorMapper.selectByPublishAndStudent(publishId, studentId);
        if (monitor == null) {
            throw new BizException(ErrorCode.EXAM_SESSION_NOT_FOUND);
        }
        if ("SUBMITTED".equals(monitor.getSessionStatus())) {
            throw new BizException(ErrorCode.EXAM_ALREADY_SUBMITTED);
        }

        int total = countQuestions(publish.getPaperId());
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        int safePage = Math.max(1, Math.min(page, totalPages == 0 ? 1 : totalPages));

        List<PaperQuestionVO> questions = loadPagedQuestions(publish, studentId, safePage);

        ExamQuestionPageVO vo = new ExamQuestionPageVO();
        vo.setQuestions(questions);
        vo.setTotalQuestions(total);
        vo.setTotalPages(totalPages);
        vo.setCurrentPage(safePage);
        vo.setPageSize(PAGE_SIZE);
        return vo;
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private ExamPublish requireActiveExam(Long publishId) {
        ExamPublish publish = publishMapper.selectById(publishId);
        if (publish == null) throw new BizException(ErrorCode.EXAM_NOT_FOUND);
        int status = ExamPublishServiceImpl.computeStatus(publish.getStartTime(), publish.getEndTime());
        if (status != 1) throw new BizException(ErrorCode.EXAM_NOT_ACTIVE);
        return publish;
    }

    private int countQuestions(Long paperId) {
        Long count = paperQuestionMapper.selectCount(
                new LambdaQueryWrapper<ExamPaperQuestion>().eq(ExamPaperQuestion::getPaperId, paperId));
        return count == null ? 0 : count.intValue();
    }

    /**
     * 加载指定页的题目，隐藏答案字段（考试进行中始终不展示）。
     * shuffleQuestion=1 时按学号+publishId 确定性洗牌，保证同学生刷新后顺序一致。
     */
    private List<PaperQuestionVO> loadPagedQuestions(ExamPublish publish, Long studentId, int page) {
        List<ExamPaperQuestion> allRelations = paperQuestionMapper.selectByPaperId(publish.getPaperId());
        if (allRelations.isEmpty()) return Collections.emptyList();

        // shuffleQuestion=1：确定性洗牌（学号 XOR publishId 作为种子）
        if (Integer.valueOf(1).equals(publish.getShuffleQuestion())) {
            long seed = studentId ^ publish.getId();
            Collections.shuffle(allRelations, new Random(seed));
        }

        // 分页截取
        int fromIndex = (page - 1) * PAGE_SIZE;
        if (fromIndex >= allRelations.size()) return Collections.emptyList();
        int toIndex = Math.min(fromIndex + PAGE_SIZE, allRelations.size());
        List<ExamPaperQuestion> pageRelations = allRelations.subList(fromIndex, toIndex);

        // 批量加载题目（避免 N+1）
        Set<Long> questionIds = pageRelations.stream()
                .map(ExamPaperQuestion::getQuestionId).collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        Set<Long> objectiveIds = questionMap.values().stream()
                .filter(q -> isObjectiveType(q.getType()))
                .map(Question::getId).collect(Collectors.toSet());
        Map<Long, List<QuestionOption>> optionsMap = new HashMap<>();
        if (!objectiveIds.isEmpty()) {
            questionOptionMapper.selectList(
                    new LambdaQueryWrapper<QuestionOption>()
                            .in(QuestionOption::getQuestionId, objectiveIds)
                            .orderByAsc(QuestionOption::getSortOrder))
                    .forEach(opt -> optionsMap
                            .computeIfAbsent(opt.getQuestionId(), k -> new ArrayList<>())
                            .add(opt));
        }

        boolean shuffleOption = Integer.valueOf(1).equals(publish.getShuffleOption());
        long optSeed = studentId ^ (publish.getId() * 31L);

        return pageRelations.stream().map(rel -> {
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
                // 考试进行中：始终隐藏答案/解析
                qVO.setAnswer(null);
                qVO.setAnalysis(null);

                if (isObjectiveType(q.getType())) {
                    List<QuestionOption> opts = new ArrayList<>(
                            optionsMap.getOrDefault(q.getId(), List.of()));
                    if (shuffleOption) {
                        Collections.shuffle(opts, new Random(optSeed ^ q.getId()));
                    }
                    List<QuestionOptionVO> optVOs = opts.stream().map(opt -> {
                        QuestionOptionVO optVO = questionConverter.toOptionVO(opt);
                        optVO.setIsCorrect(null); // 考试中隐藏正确答案标记
                        return optVO;
                    }).collect(Collectors.toList());
                    qVO.setOptions(optVOs);
                }
                pqVO.setQuestion(qVO);
            }
            return pqVO;
        }).collect(Collectors.toList());
    }

    private static boolean isObjectiveType(Integer type) {
        return type != null && List.of(1, 2, 3, 6).contains(type);
    }
}
