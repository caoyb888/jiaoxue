package cn.smu.edu.ai.service;

import cn.smu.edu.ai.domain.model.AiRequest;
import cn.smu.edu.ai.domain.model.ModelType;
import cn.smu.edu.ai.security.PromptSecurityFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * AI 网关服务 — 统一 LLM 调用入口
 *
 * 所有 LLM 调用必须经过本服务，直接调用 ChatClient 违反 C4 约束。
 * mockMode=true 时跳过真实 LLM 调用（开发/CI 环境使用）。
 */
@Slf4j
@Service
public class AiGatewayService {

    private final ChatClient analysisChatClient;
    private final ChatClient generationChatClient;
    private final ChatClient reviewChatClient;
    private final PromptSecurityFilter securityFilter;

    @Value("${ai.gateway.mock-mode:true}")
    private boolean mockMode;

    public AiGatewayService(
            @Qualifier("analysisChatClient") ChatClient analysisChatClient,
            @Qualifier("generationChatClient") ChatClient generationChatClient,
            @Qualifier("reviewChatClient") ChatClient reviewChatClient,
            PromptSecurityFilter securityFilter) {
        this.analysisChatClient = analysisChatClient;
        this.generationChatClient = generationChatClient;
        this.reviewChatClient = reviewChatClient;
        this.securityFilter = securityFilter;
    }

    /**
     * SSE 流式输出（C4：必须过安全层）
     */
    public Flux<String> chat(AiRequest request) {
        AiRequest safeReq = securityFilter.filter(request);

        if (mockMode) {
            log.debug("AI Gateway mock 模式，跳过真实 LLM 调用: modelType={}", safeReq.getModelType());
            return mockStream(safeReq);
        }

        ChatClient client = selectClient(safeReq.getModelType());
        return client.prompt()
                .system(safeReq.getSystemPrompt())
                .user(safeReq.getUserPrompt())
                .stream()
                .content()
                .map(chunk -> securityFilter.filterOutput(chunk));
    }

    /**
     * 同步调用（AI 任务 Consumer 内部使用，C4：必须过安全层）
     */
    public String chatSync(AiRequest request) {
        AiRequest safeReq = securityFilter.filter(request);

        if (mockMode) {
            log.debug("AI Gateway mock 模式，跳过真实 LLM 调用: modelType={}", safeReq.getModelType());
            return mockResponse(safeReq);
        }

        ChatClient client = selectClient(safeReq.getModelType());
        String response = client.prompt()
                .system(safeReq.getSystemPrompt())
                .user(safeReq.getUserPrompt())
                .call()
                .content();

        return securityFilter.filterOutput(response);
    }

    private ChatClient selectClient(ModelType modelType) {
        return switch (modelType) {
            case ANALYSIS   -> analysisChatClient;
            case GENERATION -> generationChatClient;
            case REVIEW     -> reviewChatClient;
        };
    }

    private Flux<String> mockStream(AiRequest req) {
        String[] chunks = {"[Mock] 这是", "AI教学助手", "的模拟回复。", "生产环境", "请配置真实API密钥。"};
        return Flux.fromArray(chunks);
    }

    private String mockResponse(AiRequest req) {
        return String.format("[Mock AI Response] 针对您的请求（%s），这是模拟的AI回复内容。生产环境请配置真实API密钥。",
                req.getUserPrompt() != null ? req.getUserPrompt().substring(0, Math.min(20, req.getUserPrompt().length())) : "");
    }
}
