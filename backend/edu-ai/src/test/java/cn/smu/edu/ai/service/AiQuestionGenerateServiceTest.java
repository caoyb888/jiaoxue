package cn.smu.edu.ai.service;

import cn.smu.edu.ai.domain.document.AiQuestionTask;
import cn.smu.edu.ai.domain.dto.QuestionGenerateDTO;
import cn.smu.edu.ai.domain.entity.GeneratedQuestion;
import cn.smu.edu.ai.repository.AiQuestionTaskRepository;
import cn.smu.edu.ai.repository.QuestionWriteMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiQuestionGenerateServiceTest {

    @Mock AiQuestionTaskRepository taskRepository;
    @Mock QuestionWriteMapper questionWriteMapper;
    @Mock AiGatewayService aiGatewayService;
    @Mock AiNotifyPublisher notifyPublisher;

    private AiQuestionGenerateService service() {
        return new AiQuestionGenerateService(taskRepository, questionWriteMapper,
                aiGatewayService, notifyPublisher, new ObjectMapper());
    }

    private AiQuestionTask task(String taskId) {
        AiQuestionTask t = new AiQuestionTask();
        t.setTaskId(taskId);
        t.setBankId(5L);
        t.setCreatorId(9L);
        t.setTopic("TCP/IP");
        t.setTypes(List.of(1, 5));
        t.setCount(2);
        t.setDifficulty(3);
        return t;
    }

    @Test
    void createTask_shouldSavePendingAndReturnTaskId() {
        QuestionGenerateDTO dto = new QuestionGenerateDTO();
        dto.setBankId(5L);
        dto.setTopic("TCP");
        dto.setTypes(List.of(1));
        dto.setCount(3);
        dto.setDifficulty(2);

        String taskId = service().createTask(dto, 9L);

        assertThat(taskId).isNotBlank();
        ArgumentCaptor<AiQuestionTask> cap = ArgumentCaptor.forClass(AiQuestionTask.class);
        verify(taskRepository).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(AiQuestionTask.STATUS_PENDING);
        assertThat(cap.getValue().getCreatorId()).isEqualTo(9L);
    }

    @Test
    void generate_shouldParseInsertAndMarkDone() {
        when(taskRepository.findByTaskId("t1")).thenReturn(Optional.of(task("t1")));
        when(aiGatewayService.chatSync(any())).thenReturn(
                "题目如下：[{\"type\":1,\"content\":\"TCP握手几次\",\"answer\":\"三次\",\"analysis\":\"SYN/SYN-ACK/ACK\",\"difficulty\":2,\"score\":5}," +
                "{\"type\":5,\"content\":\"简述拥塞控制\",\"answer\":\"慢启动...\",\"score\":10}]");
        // 模拟 useGeneratedKeys 回填 id
        when(questionWriteMapper.insertBatch(anyList())).thenAnswer(inv -> {
            List<GeneratedQuestion> list = inv.getArgument(0);
            for (int i = 0; i < list.size(); i++) list.get(i).setId(100L + i);
            return list.size();
        });

        service().generate("t1");

        ArgumentCaptor<List<GeneratedQuestion>> qCap = ArgumentCaptor.forClass(List.class);
        verify(questionWriteMapper).insertBatch(qCap.capture());
        assertThat(qCap.getValue()).hasSize(2);
        assertThat(qCap.getValue().get(0).getBankId()).isEqualTo(5L);
        assertThat(qCap.getValue().get(0).getCreatorId()).isEqualTo(9L);

        // 任务置 DONE 并记录 ids
        ArgumentCaptor<AiQuestionTask> tCap = ArgumentCaptor.forClass(AiQuestionTask.class);
        verify(taskRepository, atLeastOnce()).save(tCap.capture());
        AiQuestionTask saved = tCap.getValue();
        assertThat(saved.getStatus()).isEqualTo(AiQuestionTask.STATUS_DONE);
        assertThat(saved.getGeneratedCount()).isEqualTo(2);
        assertThat(saved.getQuestionIds()).containsExactly(100L, 101L);
        verify(notifyPublisher).notifyUser(eq(9L), eq("AI_QUESTION_DONE"), anyString(), any());
    }

    @Test
    void generate_shouldMarkFailed_whenNoValidQuestions() {
        when(taskRepository.findByTaskId("t2")).thenReturn(Optional.of(task("t2")));
        when(aiGatewayService.chatSync(any())).thenReturn("[Mock AI Response] 无JSON数组");

        service().generate("t2");

        verify(questionWriteMapper, never()).insertBatch(anyList());
        ArgumentCaptor<AiQuestionTask> tCap = ArgumentCaptor.forClass(AiQuestionTask.class);
        verify(taskRepository, atLeastOnce()).save(tCap.capture());
        assertThat(tCap.getValue().getStatus()).isEqualTo(AiQuestionTask.STATUS_FAILED);
        verify(notifyPublisher).notifyUser(eq(9L), eq("AI_QUESTION_FAILED"), anyString(), any());
    }

    @Test
    void generate_shouldSkipMissing_whenTaskNotFound() {
        when(taskRepository.findByTaskId("none")).thenReturn(Optional.empty());

        service().generate("none");

        verify(aiGatewayService, never()).chatSync(any());
        verify(questionWriteMapper, never()).insertBatch(anyList());
    }
}
