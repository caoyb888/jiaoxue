package cn.smu.edu.ai.domain.vo;

import cn.smu.edu.ai.domain.document.AiQuestionTask;
import lombok.Data;

import java.util.List;

/**
 * 一键出题任务状态（供前端 S6-13 轮询进度）。
 */
@Data
public class QuestionTaskVO {

    private String taskId;
    private Long bankId;
    private String status;
    private int generatedCount;
    private List<Long> questionIds;
    private String errorMsg;

    public static QuestionTaskVO from(AiQuestionTask task) {
        QuestionTaskVO vo = new QuestionTaskVO();
        vo.setTaskId(task.getTaskId());
        vo.setBankId(task.getBankId());
        vo.setStatus(task.getStatus());
        vo.setGeneratedCount(task.getGeneratedCount());
        vo.setQuestionIds(task.getQuestionIds());
        vo.setErrorMsg(task.getErrorMsg());
        return vo;
    }
}
