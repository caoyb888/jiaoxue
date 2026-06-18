package cn.smu.edu.ai.controller;

import cn.smu.edu.ai.domain.dto.ChatMessageDTO;
import cn.smu.edu.ai.domain.dto.DialogueTaskDTO;
import cn.smu.edu.ai.domain.model.AiRequest;
import cn.smu.edu.ai.domain.vo.DialogueTaskVO;
import cn.smu.edu.ai.security.PromptSecurityException;
import cn.smu.edu.ai.service.AiGatewayService;
import cn.smu.edu.common.aop.OperationLog;
import cn.smu.edu.common.result.Result;
import cn.smu.edu.common.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai/dialogue")
@RequiredArgsConstructor
public class DialogueController {

    private final AiGatewayService aiGatewayService;
    private final ExecutorService sseExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @OperationLog(module = "ai", operation = "创建AI对话任务")
    @PostMapping("/task")
    public Result<DialogueTaskVO> createTask(@RequestBody @Valid DialogueTaskDTO dto) {
        String sessionId = UUID.randomUUID().toString();

        // opening 也经过安全层校验（创建时写入 Redis session 等 S6 阶段补全）
        DialogueTaskVO vo = DialogueTaskVO.builder()
                .sessionId(sessionId)
                .topic(dto.getTopic())
                .opening(dto.getOpening() != null ? dto.getOpening() : "请就本课主题与我展开讨论...")
                .maxTurns(dto.getMaxTurns())
                .build();
        return Result.ok(vo);
    }

    /**
     * SSE 流式输出（C4：经 AiGatewayService 强制过安全层）
     * 返回 text/event-stream，网关层关闭全缓冲透传流
     */
    @PostMapping(value = "/{sessionId}/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@PathVariable String sessionId,
                                  @RequestBody @Valid ChatMessageDTO dto) {
        Long userId = UserContext.getUserId();
        SseEmitter emitter = new SseEmitter(60_000L);

        AiRequest request = AiRequest.builder()
                .userPrompt(dto.getContent())
                .userId(userId)
                .build();

        sseExecutor.submit(() -> {
            Disposable subscription = null;
            try {
                var flux = aiGatewayService.chat(request);
                subscription = flux.subscribe(
                        chunk -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .data("{\"type\":\"chunk\",\"content\":\"" + escapeJson(chunk) + "\"}"));
                            } catch (IOException e) {
                                log.warn("SSE 发送 chunk 失败: sessionId={}", sessionId);
                            }
                        },
                        error -> {
                            try {
                                if (error instanceof PromptSecurityException pse) {
                                    emitter.send(SseEmitter.event()
                                            .data("{\"type\":\"error\",\"code\":" + pse.getCode() + ",\"message\":\"" + escapeJson(error.getMessage()) + "\"}"));
                                }
                                emitter.completeWithError(error);
                            } catch (IOException ex) {
                                emitter.completeWithError(ex);
                            }
                        },
                        () -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .data("{\"type\":\"done\",\"content\":\"\"}"));
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                );
            } catch (PromptSecurityException pse) {
                try {
                    emitter.send(SseEmitter.event()
                            .data("{\"type\":\"error\",\"code\":" + pse.getCode() + ",\"message\":\"" + escapeJson(pse.getMessage()) + "\"}"));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @GetMapping("/{sessionId}/history")
    public Result<?> getHistory(@PathVariable String sessionId) {
        // S6 阶段完整实现（需 MongoDB 存对话历史）
        return Result.ok(java.util.List.of());
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
