package cn.smu.edu.ai.service;

import cn.smu.edu.ai.domain.document.AiLectureTranscript;
import cn.smu.edu.ai.repository.AiLectureTranscriptRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonSummaryServiceTest {

    @Mock AiLectureTranscriptRepository transcriptRepository;
    @Mock AiGatewayService aiGatewayService;

    private LessonSummaryService service() {
        return new LessonSummaryService(transcriptRepository, aiGatewayService, new ObjectMapper());
    }

    private AiLectureTranscript transcript(String fullText) {
        AiLectureTranscript t = new AiLectureTranscript();
        t.setLessonId(1L);
        t.setFullText(fullText);
        return t;
    }

    @Test
    void loadTranscript_shouldReturnFullText() {
        when(transcriptRepository.findByLessonId(1L)).thenReturn(Optional.of(transcript("今天讲了TCP")));
        assertThat(service().loadTranscript(1L)).isEqualTo("今天讲了TCP");
    }

    @Test
    void loadTranscript_shouldReturnNull_whenMissingOrBlank() {
        when(transcriptRepository.findByLessonId(1L)).thenReturn(Optional.empty());
        assertThat(service().loadTranscript(1L)).isNull();

        when(transcriptRepository.findByLessonId(2L)).thenReturn(Optional.of(transcript("   ")));
        assertThat(service().loadTranscript(2L)).isNull();
    }

    @Test
    void summarize_shouldParseSummaryAndKeyPoints() {
        when(aiGatewayService.chatSync(any())).thenReturn(
                "{\"summary\": \"本节课讲解TCP三次握手\", \"keyPoints\": [\"SYN\", \"SYN-ACK\", \"ACK\"]}");

        LessonSummaryService.SummaryResult r = service().summarize(1L, "课堂转写内容");

        assertThat(r.summary()).isEqualTo("本节课讲解TCP三次握手");
        assertThat(r.keyPoints()).containsExactly("SYN", "SYN-ACK", "ACK");
        assertThat(r.keyPointsJson()).contains("SYN-ACK");
    }

    @Test
    void summarize_shouldTruncateSummaryTo500Chars() {
        String longSummary = "字".repeat(800);
        when(aiGatewayService.chatSync(any()))
                .thenReturn("{\"summary\": \"" + longSummary + "\", \"keyPoints\": []}");

        LessonSummaryService.SummaryResult r = service().summarize(1L, "内容");

        assertThat(r.summary()).hasSize(LessonSummaryService.SUMMARY_MAX_CHARS);
        assertThat(r.keyPoints()).isEmpty();
        assertThat(r.keyPointsJson()).isEqualTo("[]");
    }

    @Test
    void summarize_shouldFallbackToPlainText_whenInvalidJson() {
        when(aiGatewayService.chatSync(any())).thenReturn("[Mock AI Response] 纯文本无JSON");

        LessonSummaryService.SummaryResult r = service().summarize(1L, "内容");

        assertThat(r.summary()).contains("Mock AI Response");
        assertThat(r.keyPoints()).isEmpty();
        assertThat(r.keyPointsJson()).isEqualTo("[]");
    }

    @Test
    void summarize_shouldStillCallLlm_whenNoTranscript() {
        when(aiGatewayService.chatSync(any())).thenReturn("{\"summary\": \"无转写\", \"keyPoints\": []}");

        LessonSummaryService.SummaryResult r = service().summarize(1L, null);

        assertThat(r.summary()).isEqualTo("无转写");
    }
}
