package cn.smu.edu.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 多模型 ChatClient 配置（Spring AI 1.0.0）
 *
 * 路由策略：
 *   analysisChatClient   → 文心4（ERNIE-4.0，逻辑推理/分析）
 *   generationChatClient → 通义Qwen-Max（创意生成/摘要）
 *   reviewChatClient     → GPT-4o（主观题批改，精度最高）
 *
 * 开发环境：三者均指向同一 OpenAI-compatible 端点，通过 model 参数区分
 * 生产环境：通过 Nacos 注入各自的 baseUrl + apiKey
 */
@Configuration
public class AiModelConfig {

    @Value("${ai.model.analysis.base-url:https://api.openai.com/v1}")
    private String analysisBaseUrl;
    @Value("${ai.model.analysis.api-key:}")
    private String analysisApiKey;
    @Value("${ai.model.analysis.model:ernie-4.0}")
    private String analysisModel;

    @Value("${ai.model.generation.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String generationBaseUrl;
    @Value("${ai.model.generation.api-key:}")
    private String generationApiKey;
    @Value("${ai.model.generation.model:qwen-max}")
    private String generationModel;

    @Value("${ai.model.review.base-url:https://api.openai.com/v1}")
    private String reviewBaseUrl;
    @Value("${ai.model.review.api-key:}")
    private String reviewApiKey;
    @Value("${ai.model.review.model:gpt-4o}")
    private String reviewModel;

    @Primary
    @Bean("analysisChatClient")
    public ChatClient analysisChatClient() {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(analysisBaseUrl)
                .apiKey(analysisApiKey)
                .build();
        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(analysisModel).build())
                .build();
        return ChatClient.builder(model).build();
    }

    @Bean("generationChatClient")
    public ChatClient generationChatClient() {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(generationBaseUrl)
                .apiKey(generationApiKey)
                .build();
        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(generationModel).build())
                .build();
        return ChatClient.builder(model).build();
    }

    @Bean("reviewChatClient")
    public ChatClient reviewChatClient() {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(reviewBaseUrl)
                .apiKey(reviewApiKey)
                .build();
        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(reviewModel).build())
                .build();
        return ChatClient.builder(model).build();
    }
}
