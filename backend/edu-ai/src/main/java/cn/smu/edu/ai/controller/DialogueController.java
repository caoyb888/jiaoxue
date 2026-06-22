package cn.smu.edu.ai.controller;

import cn.smu.edu.ai.domain.document.AiDialogueSession;
import cn.smu.edu.ai.domain.dto.ChatMessageDTO;
import cn.smu.edu.ai.domain.dto.DialogueTaskDTO;
import cn.smu.edu.ai.domain.model.AiRequest;
import cn.smu.edu.ai.domain.model.ModelType;
import cn.smu.edu.ai.domain.vo.DialogueMessageVO;
import cn.smu.edu.ai.domain.vo.DialogueTaskVO;
import cn.smu.edu.ai.security.PromptSecurityException;
import cn.smu.edu.ai.service.AiGatewayService;
import cn.smu.edu.ai.service.DialogueService;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai/dialogue")
@RequiredArgsConstructor
public class DialogueController {

    private final AiGatewayService aiGatewayService;
    private final DialogueService dialogueService;
    private final ExecutorService sseExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @OperationLog(module = "ai", operation = "创建AI对话任务")
    @PostMapping("/task")
    public Result<DialogueTaskVO> createTask(@RequestBody @Valid DialogueTaskDTO dto) {
        Long userId = UserContext.getUserId();
        AiDialogueSession session = dialogueService.createSession(dto, userId);
        DialogueTaskVO vo = DialogueTaskVO.builder()
                .sessionId(session.getSessionId())
                .topic(session.getTopic())
                .opening(session.getOpening())
                .maxTurns(session.getMaxTurns())
                .build();
        return Result.ok(vo);
    }

    /**
     * SSE 流式输出（C4：经 AiGatewayService 强制过安全层）
     * 返回 text/event-stream，网关层关闭全缓冲透传流（edu-ai-dialogue 路由 response-timeout=-1）。
     * 学生发言落库 → 流式回复聚合后落库 assistant 消息。
     */
    @PostMapping(value = "/{sessionId}/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(@PathVariable String sessionId,
                                  @RequestBody @Valid ChatMessageDTO dto) {
        Long userId = UserContext.getUserId();
        SseEmitter emitter = new SseEmitter(0L); // 0 = 不超时，由流自身完成/出错收尾

        AiDialogueSession session = dialogueService.getSession(sessionId);
        if (session == null) {
            sendErrorAndComplete(emitter, 404, "对话会话不存在");
            return emitter;
        }
        if (session.getTurnCount() >= session.getMaxTurns()) {
            sendErrorAndComplete(emitter, 200720, "已达最大对话轮次");
            return emitter;
        }

        // 学生发言先落库（轮次 +1）
        final String userMsgId = dialogueService.saveUserMessage(sessionId, userId, dto.getContent()).getId();

        AiRequest request = AiRequest.builder()
                .userPrompt(dto.getContent())
                .userId(userId)
                .modelType(parseModel(session.getModelType()))
                .systemPrompt("你是课堂学习助手，围绕主题「" + session.getTopic() + "」与学生展开有引导性的讨论。")
                .build();

        StringBuilder full = new StringBuilder();
        sseExecutor.submit(() -> {
            Disposable subscription = null;
            try {
                subscription = aiGatewayService.chat(request).subscribe(
                        chunk -> {
                            full.append(chunk);
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
                                    dialogueService.markFiltered(userMsgId);
                                    emitter.send(SseEmitter.event()
                                            .data("{\"type\":\"error\",\"code\":" + pse.getCode() + ",\"message\":\"" + escapeJson(error.getMessage()) + "\"}"));
                                }
                                emitter.completeWithError(error);
                            } catch (IOException ex) {
                                emitter.completeWithError(ex);
                            }
                        },
                        () -> {
                            // 流式回复聚合后落库 assistant 消息
                            if (!full.isEmpty()) {
                                dialogueService.saveAssistantMessage(sessionId, userId, full.toString());
                            }
                            try {
                                emitter.send(SseEmitter.event().data("{\"type\":\"done\",\"content\":\"\"}"));
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                );
            } catch (PromptSecurityException pse) {
                // 输入被安全层拦截：消息落库标记 is_filtered=true，不调用 LLM
                dialogueService.markFiltered(userMsgId);
                sendErrorAndComplete(emitter, pse.getCode(), pse.getMessage());
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @GetMapping("/{sessionId}/history")
    public Result<List<DialogueMessageVO>> getHistory(@PathVariable String sessionId) {
        List<DialogueMessageVO> list = dialogueService.history(sessionId)
                .stream().map(DialogueMessageVO::from).toList();
        return Result.ok(list);
    }

    private ModelType parseModel(String name) {
        try {
            return name != null ? ModelType.valueOf(name) : ModelType.ANALYSIS;
        } catch (IllegalArgumentException e) {
            return ModelType.ANALYSIS;
        }
    }

    private void sendErrorAndComplete(SseEmitter emitter, int code, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .data("{\"type\":\"error\",\"code\":" + code + ",\"message\":\"" + escapeJson(message) + "\"}"));
            emitter.complete();
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
