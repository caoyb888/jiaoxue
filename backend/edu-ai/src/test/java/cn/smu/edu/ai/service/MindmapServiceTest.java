package cn.smu.edu.ai.service;

import cn.smu.edu.ai.domain.document.AiMindmap;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MindmapServiceTest {

    @Mock AiGatewayService aiGatewayService;
    @Mock MongoTemplate mongoTemplate;

    private MindmapService service() {
        return new MindmapService(aiGatewayService, mongoTemplate, new ObjectMapper());
    }

    @Test
    void generate_shouldReturnValidJsonAndUpsertMongo() {
        String json = "{\"title\":\"TCP\",\"children\":[{\"content\":\"三次握手\",\"children\":[]}]}";
        when(aiGatewayService.chatSync(any())).thenReturn("思维导图如下：" + json);

        String result = service().generate(1L, 9L, "课堂转写", "MINDMAP");

        assertThat(result).isEqualTo(json);
        ArgumentCaptor<Update> cap = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).upsert(any(), cap.capture(), eq(AiMindmap.class));
        org.bson.Document set = cap.getValue().getUpdateObject().get("$set", org.bson.Document.class);
        assertThat(set.getString("markmapJson")).isEqualTo(json);
        assertThat(set.getBoolean("parsed")).isTrue();
        assertThat(set.getString("source")).isEqualTo("MINDMAP");
    }

    @Test
    void generate_shouldFallbackToPlaceholder_whenInvalidJson() {
        when(aiGatewayService.chatSync(any())).thenReturn("[Mock AI Response] 无JSON");

        String result = service().generate(2L, 9L, "内容", "SUMMARY");

        assertThat(result).contains("思维导图生成失败");
        ArgumentCaptor<Update> cap = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).upsert(any(), cap.capture(), eq(AiMindmap.class));
        org.bson.Document set = cap.getValue().getUpdateObject().get("$set", org.bson.Document.class);
        assertThat(set.getBoolean("parsed")).isFalse();
    }

    @Test
    void saveEdited_shouldUpsertWithEditedSource() {
        String json = "{\"title\":\"编辑后\",\"children\":[]}";

        service().saveEdited(10L, 9L, json);

        ArgumentCaptor<Update> cap = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).upsert(any(), cap.capture(), eq(AiMindmap.class));
        org.bson.Document set = cap.getValue().getUpdateObject().get("$set", org.bson.Document.class);
        assertThat(set.getString("markmapJson")).isEqualTo(json);
        assertThat(set.getString("source")).isEqualTo("EDITED");
    }

    @Test
    void saveEdited_shouldThrow_whenInvalidJson() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service().saveEdited(10L, 9L, "不是JSON"));
    }

    @Test
    void generate_shouldStillCallLlm_whenNoTranscript() {
        when(aiGatewayService.chatSync(any())).thenReturn("{\"title\":\"t\",\"children\":[]}");

        String result = service().generate(3L, 9L, null, "MINDMAP");

        assertThat(result).contains("title");
        verify(mongoTemplate).upsert(any(), any(Update.class), eq(AiMindmap.class));
    }
}
