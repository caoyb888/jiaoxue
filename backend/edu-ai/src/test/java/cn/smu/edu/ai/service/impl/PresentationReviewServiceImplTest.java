package cn.smu.edu.ai.service.impl;

import cn.smu.edu.ai.domain.document.AiPresentationReview;
import cn.smu.edu.ai.domain.dto.PresentationReviewDTO;
import cn.smu.edu.ai.domain.model.AiRequest;
import cn.smu.edu.ai.domain.model.ModelType;
import cn.smu.edu.ai.repository.AiPresentationReviewRepository;
import cn.smu.edu.ai.service.AiGatewayService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresentationReviewServiceImplTest {

    @Mock
    AiPresentationReviewRepository repository;
    @Mock
    AiGatewayService aiGatewayService;

    private PresentationReviewServiceImpl service() {
        return new PresentationReviewServiceImpl(repository, aiGatewayService, new ObjectMapper());
    }

    private PresentationReviewDTO dto() {
        PresentationReviewDTO d = new PresentationReviewDTO();
        d.setLessonId(1L);
        d.setStudentId(10L);
        d.setStudentName("张三");
        d.setTranscript("各位老师同学好，我汇报的主题是……");
        return d;
    }

    @Test
    void review_defaultRules_shouldParseScoresAndComputeWeightedTotal() {
        when(repository.findByLessonIdAndStudentId(1L, 10L)).thenReturn(Optional.empty());
        // 默认权重 0.4/0.3/0.2/0.1，分数 90/80/70/60 → 90*.4+80*.3+70*.2+60*.1=80.0
        when(aiGatewayService.chatSync(any(AiRequest.class))).thenReturn("""
                ```json
                {"dimensions":[
                  {"name":"内容质量","score":90,"comment":"内容充实"},
                  {"name":"逻辑结构","score":80,"comment":"结构清晰"},
                  {"name":"语言表达","score":70,"comment":"表达流畅"},
                  {"name":"时间控制","score":60,"comment":"略超时"}],
                 "overallComment":"整体良好"}
                ```
                """);

        AiPresentationReview r = service().review(dto());

        assertThat(r.isParsed()).isTrue();
        assertThat(r.getTotalScore()).isEqualTo(80.0);
        assertThat(r.getOverallComment()).isEqualTo("整体良好");
        assertThat(r.getDimensions()).hasSize(4);
        assertThat(r.getDimensions().get(0).getName()).isEqualTo("内容质量");
        assertThat(r.getDimensions().get(0).getScore()).isEqualTo(90.0);

        // REVIEW 模型 + prompt 含转写
        ArgumentCaptor<AiRequest> cap = ArgumentCaptor.forClass(AiRequest.class);
        org.mockito.Mockito.verify(aiGatewayService).chatSync(cap.capture());
        assertThat(cap.getValue().getModelType()).isEqualTo(ModelType.REVIEW);
        assertThat(cap.getValue().getUserPrompt()).contains("汇报的主题");
    }

    @Test
    void review_customRules_shouldUseProvidedDimensions() {
        when(repository.findByLessonIdAndStudentId(1L, 10L)).thenReturn(Optional.empty());
        when(aiGatewayService.chatSync(any(AiRequest.class))).thenReturn(
                "{\"dimensions\":[{\"name\":\"专业性\",\"score\":50,\"comment\":\"ok\"}],\"overallComment\":\"x\"}");

        PresentationReviewDTO d = dto();
        PresentationReviewDTO.DimensionRule r1 = new PresentationReviewDTO.DimensionRule();
        r1.setName("专业性");
        r1.setWeight(1.0);
        r1.setMaxScore(60);
        d.setDimensions(List.of(r1));

        AiPresentationReview r = service().review(d);

        assertThat(r.getDimensions()).hasSize(1);
        assertThat(r.getDimensions().get(0).getName()).isEqualTo("专业性");
        assertThat(r.getTotalScore()).isEqualTo(50.0); // 50*1.0
    }

    @Test
    void review_scoreOverMax_shouldBeClamped() {
        when(repository.findByLessonIdAndStudentId(1L, 10L)).thenReturn(Optional.empty());
        PresentationReviewDTO d = dto();
        PresentationReviewDTO.DimensionRule r1 = new PresentationReviewDTO.DimensionRule();
        r1.setName("内容");
        r1.setWeight(1.0);
        r1.setMaxScore(100);
        d.setDimensions(List.of(r1));
        when(aiGatewayService.chatSync(any(AiRequest.class))).thenReturn(
                "{\"dimensions\":[{\"name\":\"内容\",\"score\":150,\"comment\":\"超分\"}]}");

        AiPresentationReview r = service().review(d);

        assertThat(r.getDimensions().get(0).getScore()).isEqualTo(100.0); // 截到满分
    }

    @Test
    void review_nonJsonOutput_shouldDegradeGracefully() {
        when(repository.findByLessonIdAndStudentId(1L, 10L)).thenReturn(Optional.empty());
        when(aiGatewayService.chatSync(any(AiRequest.class))).thenReturn("[Mock AI Response] 无法解析的纯文本");

        AiPresentationReview r = service().review(dto());

        assertThat(r.isParsed()).isFalse();
        assertThat(r.getTotalScore()).isEqualTo(0.0);
        assertThat(r.getOverallComment()).contains("无法解析");
        assertThat(r.getDimensions()).hasSize(4); // 仍按默认规则列出，分数 0
    }
}
