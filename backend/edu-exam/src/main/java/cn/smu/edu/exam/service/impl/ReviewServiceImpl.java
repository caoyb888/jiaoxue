package cn.smu.edu.exam.service.impl;

import cn.smu.edu.common.exception.BizException;
import cn.smu.edu.common.result.ErrorCode;
import cn.smu.edu.exam.domain.dto.ReviewAnswerDTO;
import cn.smu.edu.exam.domain.entity.ExamAnswerAttachment;
import cn.smu.edu.exam.domain.entity.ExamPublish;
import cn.smu.edu.exam.domain.entity.StudentAnswer;
import cn.smu.edu.exam.domain.vo.StudentAnswerVO;
import cn.smu.edu.exam.repository.ExamAnswerAttachmentMapper;
import cn.smu.edu.exam.repository.ExamPublishMapper;
import cn.smu.edu.exam.repository.StudentAnswerMapper;
import cn.smu.edu.exam.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final StudentAnswerMapper studentAnswerMapper;
    private final ExamPublishMapper publishMapper;
    private final ExamAnswerAttachmentMapper attachmentMapper;

    @Override
    @Transactional
    public StudentAnswerVO review(Long answerId, Long teacherId, ReviewAnswerDTO dto) {
        StudentAnswer answer = studentAnswerMapper.selectById(answerId);
        if (answer == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "作答记录不存在");
        }

        // 验证教师有权批改本场考试（teacherId 需与 exam_publish.teacher_id 匹配）
        ExamPublish publish = publishMapper.selectById(answer.getPublishId());
        if (publish == null || !teacherId.equals(publish.getTeacherId())) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "无权批改此作答");
        }

        // 已是教师批改状态，允许再次修改（review_status 保持2）
        answer.setScore(dto.getScore());
        answer.setIsCorrect(dto.getIsCorrect());
        answer.setComment(dto.getComment());
        answer.setReviewStatus(2); // 2=教师已批改
        studentAnswerMapper.updateById(answer);

        log.info("教师批改完成: answerId={}, teacherId={}, score={}", answerId, teacherId, dto.getScore());
        return toVO(answer);
    }

    @Override
    public List<StudentAnswerVO> listAnswers(Long publishId, Long studentId) {
        List<StudentAnswer> answers = studentAnswerMapper.selectByPublishAndStudent(publishId, studentId);
        return answers.stream().map(this::toVO).collect(Collectors.toList());
    }

    private StudentAnswerVO toVO(StudentAnswer a) {
        StudentAnswerVO vo = new StudentAnswerVO();
        vo.setId(a.getId());
        vo.setPublishId(a.getPublishId());
        vo.setQuestionId(a.getQuestionId());
        vo.setStudentId(a.getStudentId());
        vo.setAnswerContent(a.getAnswerContent());
        vo.setScore(a.getScore());
        vo.setIsCorrect(a.getIsCorrect());
        vo.setComment(a.getComment());
        vo.setReviewStatus(a.getReviewStatus());
        vo.setSubmittedAt(a.getSubmittedAt());
        if (a.getId() != null) {
            List<String> attachments = attachmentMapper.selectByStudentAnswerId(a.getId())
                    .stream().map(ExamAnswerAttachment::getFileKey).collect(Collectors.toList());
            if (!attachments.isEmpty()) vo.setAttachments(attachments);
        }
        return vo;
    }
}
