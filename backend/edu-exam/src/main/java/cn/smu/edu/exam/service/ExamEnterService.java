package cn.smu.edu.exam.service;

import cn.smu.edu.exam.domain.dto.ExamEnterDTO;
import cn.smu.edu.exam.domain.vo.ExamEnterVO;
import cn.smu.edu.exam.domain.vo.ExamQuestionPageVO;

public interface ExamEnterService {

    /**
     * 进入考试：密码验证 → 创建/获取监考记录 → 返回首页题目。
     * 若 faceVerifyType > 0，session_status = VERIFYING，需单独完成人脸核验（S5-04）再变为 ANSWERING。
     */
    ExamEnterVO enter(Long publishId, Long studentId, ExamEnterDTO dto);

    /**
     * 分批获取题目（每页 PAGE_SIZE 题）。
     * 若 shuffleQuestion=1，顺序按学号+发布ID确定性洗牌；答案字段始终隐藏（考试中不可见）。
     */
    ExamQuestionPageVO getQuestionsPage(Long publishId, Long studentId, int page);
}
