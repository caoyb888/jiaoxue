package cn.smu.edu.exam.service;

import cn.smu.edu.common.result.PageResult;
import cn.smu.edu.exam.domain.dto.*;
import cn.smu.edu.exam.domain.vo.ExamPaperDetailVO;
import cn.smu.edu.exam.domain.vo.ExamPaperVO;
import cn.smu.edu.exam.domain.vo.ScoreCheckVO;

public interface ExamPaperService {

    ExamPaperVO create(ExamPaperCreateDTO dto, Long creatorId);

    ExamPaperVO update(Long paperId, ExamPaperUpdateDTO dto, Long creatorId);

    void delete(Long paperId, Long creatorId);

    ExamPaperDetailVO getDetail(Long paperId, Long creatorId);

    PageResult<ExamPaperVO> list(ExamPaperQueryDTO query, Long creatorId);

    /** 批量添加题目到试卷（跳过已存在的 paper_id+question_id+paper_group 组合） */
    ExamPaperDetailVO addQuestions(Long paperId, BatchAddQuestionsDTO dto, Long creatorId);

    /** 从试卷中移除指定题目（指定卷组） */
    void removeQuestion(Long paperId, Long questionId, String paperGroup, Long creatorId);

    /** 随机组卷：按规则从题库抽题，清空指定卷组后重新填入 */
    ExamPaperDetailVO randomCompose(Long paperId, RandomCompositionDTO dto, Long creatorId);

    /** 总分校验：比较设定总分与实际题目分值之和 */
    ScoreCheckVO checkScore(Long paperId, Long creatorId);
}
