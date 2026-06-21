package cn.smu.edu.ai.service;

import cn.smu.edu.ai.domain.document.AiReviewResult;
import cn.smu.edu.ai.domain.dto.ReviewItemDTO;
import cn.smu.edu.ai.repository.AiReviewResultRepository;
import cn.smu.edu.ai.repository.ReviewQueryMapper;
import cn.smu.edu.ai.repository.ReviewWritebackMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiReviewServiceTest {

    @Mock ReviewQueryMapper reviewQueryMapper;
    @Mock AiReviewResultRepository reviewResultRepository;
    @Mock ReviewWritebackMapper reviewWritebackMapper;
    @Mock AiGatewayService aiGatewayService;

    private AiReviewService service() {
        return new AiReviewService(reviewQueryMapper, reviewResultRepository,
                reviewWritebackMapper, aiGatewayService, new ObjectMapper());
    }

    private ReviewItemDTO item(long answerId) {
        ReviewItemDTO i = new ReviewItemDTO();
        i.setAnswerId(answerId);
        i.setQuestionId(9L);
        i.setStudentId(2L);
        i.setQuestionContent("简述TCP三次握手");
        i.setReferenceAnswer("SYN/SYN-ACK/ACK");
        i.setMaxScore(new BigDecimal("10"));
        i.setStudentAnswer("客户端发SYN...");
        return i;
    }

    @Test
    void reviewByPublish_shouldParseJsonAndSave() {
        when(reviewQueryMapper.selectPendingSubjective(1L)).thenReturn(List.of(item(100L)));
        when(aiGatewayService.chatSync(any()))
                .thenReturn("{\"score\": 8, \"comment\": \"基本正确\", \"errorReason\": \"缺少ACK说明\"}");

        int done = service().reviewByPublish(1L, "task-1");

        assertThat(done).isEqualTo(1);
        ArgumentCaptor<AiReviewResult> cap = ArgumentCaptor.forClass(AiReviewResult.class);
        verify(reviewResultRepository).save(cap.capture());
        AiReviewResult r = cap.getValue();
        assertThat(r.getScore()).isEqualByComparingTo("8");
        assertThat(r.getComment()).isEqualTo("基本正确");
        assertThat(r.isParsed()).isTrue();
        assertThat(r.getAnswerId()).isEqualTo(100L);
        // S6-03：解析成功写回 student_answer
        verify(reviewWritebackMapper).writeBack(eq(100L), any(), eq("基本正确"));
    }

    @Test
    void reviewByPublish_shouldClampScoreToMax() {
        when(reviewQueryMapper.selectPendingSubjective(1L)).thenReturn(List.of(item(101L)));
        when(aiGatewayService.chatSync(any())).thenReturn("前缀文字{\"score\": 99, \"comment\": \"满分\"}后缀");

        service().reviewByPublish(1L, "task-2");

        ArgumentCaptor<AiReviewResult> cap = ArgumentCaptor.forClass(AiReviewResult.class);
        verify(reviewResultRepository).save(cap.capture());
        assertThat(cap.getValue().getScore()).isEqualByComparingTo("10"); // clamp 到 maxScore
    }

    @Test
    void reviewByPublish_shouldFallback_whenInvalidJson() {
        when(reviewQueryMapper.selectPendingSubjective(1L)).thenReturn(List.of(item(102L)));
        when(aiGatewayService.chatSync(any())).thenReturn("[Mock AI Response] 无法解析");

        service().reviewByPublish(1L, "task-3");

        ArgumentCaptor<AiReviewResult> cap = ArgumentCaptor.forClass(AiReviewResult.class);
        verify(reviewResultRepository).save(cap.capture());
        AiReviewResult r = cap.getValue();
        assertThat(r.isParsed()).isFalse();
        assertThat(r.getScore()).isNull();
        assertThat(r.getComment()).contains("人工复核");
        // 降级题不写回，保留 review_status=0 待人工
        verify(reviewWritebackMapper, never()).writeBack(any(), any(), any());
    }

    @Test
    void reviewByPublish_shouldReturnZero_whenNoPublishId() {
        assertThat(service().reviewByPublish(null, "task-4")).isZero();
        verifyNoInteractions(reviewQueryMapper, aiGatewayService, reviewResultRepository);
    }
}
